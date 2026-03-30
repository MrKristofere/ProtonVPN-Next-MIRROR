/*
 * Copyright (C) 2026 SMH01
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.protonmod.next.desktop

import kotlinx.serialization.json.Json
import ru.protonmod.next.data.network.*
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.data.repository.DesktopCertificateManager
import ru.protonmod.next.desktop.data.repository.DesktopVpnRepository
import ru.protonmod.next.desktop.monitoring.DesktopSentryManager
import ru.protonmod.next.desktop.network.DesktopDnsManager
import ru.protonmod.next.desktop.network.DesktopSplitTunnelingManager
import ru.protonmod.next.desktop.vpn.AmneziaUapiGenerator
import ru.protonmod.next.vpn.ObfuscationParams
import ru.protonmod.next.vpn.VpnConstants
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Base64

class DesktopVpnClient(
    private val database: DesktopDatabase,
    private val vpnRepository: DesktopVpnRepository,
    private val certificateManager: DesktopCertificateManager,
    private val settingsManager: DesktopSettingsManager? = null,
    private val splitTunnelingManager: DesktopSplitTunnelingManager? = null,
    private val dnsManager: DesktopDnsManager? = null,
    private val sentryManager: DesktopSentryManager? = null
) {
    private val configGenerator = AmneziaUapiGenerator()

    private var helperProcess: Process? = null

    suspend fun getServers(accessToken: String, sessionId: String, userTier: Int? = null): Result<List<ServerEntry>> {
        // Use repository which has caching and silent fallback
        val result = vpnRepository.getServers(accessToken, sessionId, userTier ?: 0)
        
        return result.map { list ->
            list.map { logical ->
                val physical = logical.servers.firstOrNull()?.copy()
                ServerEntry(
                    id = logical.id,
                    name = logical.name,
                    city = logical.city,
                    country = logical.entryCountry.ifBlank { logical.exitCountry },
                    tier = logical.tier,
                    physicalServer = physical
                )
            }
        }
    }

    suspend fun setupVpn(): Result<Unit> {
        return try {
            val session = database.getSession()
            if (session != null && !session.wgCertificate.isNullOrEmpty() && !session.wgPrivateKey.isNullOrEmpty()) {
                println("VPN setup already done, skipping API calls.")
                return Result.success(Unit)
            }

            println("Starting initial VPN setup...")
            
            // Use certificate manager to perform initial registration
            val certResult = certificateManager.forceRefreshCertificate()
            if (certResult.isSuccess) {
                println("Initial VPN setup completed successfully.")
                Result.success(Unit)
            } else {
                val error = certResult.exceptionOrNull()?.message ?: "Unknown error"
                println("Initial VPN setup failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            sentryManager?.captureException(e, "VpnClient setupVpn")
            println("VPN setup failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun connect(server: ServerEntry): Result<Unit> {
        val startTime = System.currentTimeMillis()
        
        // If already connected or connecting, disconnect first to ensure clean state
        if (helperProcess != null) {
            println("Already connected, disconnecting before reconnect...")
            disconnect()
            // Wait a bit for interface cleanup
            withContext(Dispatchers.IO) {
                Thread.sleep(1000)
            }
        }

        return try {
            // Track VPN connection attempt
            sentryManager?.trackVpnConnectionAttempt(
                server = server.name,
                country = server.country
            )
            
            val session = database.getSession() ?: throw Exception("No active session")
            
            // Check if certificate is missing or fully expired
            if (session.wgCertificate.isNullOrEmpty() || certificateManager.isEffectivelyExpired()) {
                println("Certificate missing or expired, performing setup/refresh...")
                val setupResult = setupVpn()
                if (setupResult.isFailure) {
                    // Fail if it's the very first time or it's hard-expired
                    sentryManager?.trackVpnConnectionError(
                        server = server.name,
                        errorMessage = "VPN setup failed"
                    )
                    return setupResult
                }
            }

            val privKey = database.getSession()?.wgPrivateKey ?: throw Exception("No VPN private key available")

            val physicalServer = server.physicalServer ?: throw Exception("No physical server for this logical server")
            val serverPublicKey = physicalServer.wgPublicKey ?: throw Exception("Server has no WG public key")
            val targetIp = physicalServer.exitIp ?: throw Exception("Server has no exit IP")

            // 4. Generate config
            val obfuscationParams = settingsManager?.getObfuscationParams() ?: ObfuscationParams(
                jc = 3, jmin = 1, jmax = 3, s1 = 0, s2 = 0,
                h1 = "1", h2 = "2", h3 = "3", h4 = "4",
                i1 = VpnConstants.DEFAULT_I1
            )
            
            val port = settingsManager?.settings?.value?.vpnPort ?: 1194
            
            // Get DNS settings from DNS manager
            val dnsServer = dnsManager?.getActiveDns() ?: settingsManager?.getActiveDns() ?: VpnConstants.PROTON_DNS_IP
            
            // Get split tunneling settings with domain resolution
            val splitTunnelingEnabled = splitTunnelingManager?.config?.value?.enabled ?: settingsManager?.settings?.value?.splitTunnelingEnabled ?: false
            
            var isIncludeMode = false
            var selectedApps = emptySet<String>()
            var selectedIps = emptySet<String>()
            var resolvedIpsCount = 0

            if (splitTunnelingEnabled && splitTunnelingManager != null) {
                val config = splitTunnelingManager.config.value
                isIncludeMode = config.mode == "include"
                selectedApps = config.excludedApps
                
                // Resolve domains to IPs and combine with excluded IPs
                val resolvedIps = splitTunnelingManager.resolveDomensToIps()
                resolvedIpsCount = resolvedIps.size
                
                val excludedIps = config.excludedIps
                selectedIps = resolvedIps + excludedIps
            }

            val uapiConfig = configGenerator.buildConfig(
                serverPublicKey = serverPublicKey,
                privateKey = privKey,
                localIp = VpnConstants.PROTON_CLIENT_IP,
                dnsServer = dnsServer,
                targetIp = targetIp,
                isIncludeMode = isIncludeMode,
                selectedApps = selectedApps,
                selectedIps = selectedIps,
                port = port,
                obfuscationParams = obfuscationParams
            )

            println("Generated UAPI Config:\n$uapiConfig")

            // Track split tunneling configuration
            if (splitTunnelingEnabled) {
                sentryManager?.trackSplitTunnelingConfigured(
                    enabled = true,
                    mode = if (isIncludeMode) "include" else "exclude",
                    appCount = selectedApps.size,
                    ipCount = selectedIps.size,
                    domainCount = resolvedIpsCount
                )
            }

            // Track DNS configuration
            sentryManager?.trackDnsConfigured(dnsServer = dnsServer)

            // 5. Connect via root helper daemon
            startHelper("wg0", uapiConfig, VpnConstants.PROTON_CLIENT_IP, targetIp)
            
            // Track successful connection with duration
            val durationMs = System.currentTimeMillis() - startTime
            sentryManager?.trackVpnConnectionSuccess(server = server.name, durationMs = durationMs)
            
            Result.success(Unit)
        } catch (e: Exception) {
            sentryManager?.trackVpnConnectionError(
                server = server.name,
                errorMessage = e.message ?: "Unknown error"
            )
            sentryManager?.captureException(e, "VpnClient connect")
            Result.failure(e)
        }
    }

    private suspend fun startHelper(iface: String, config: String, localIp: String, serverIp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resourceDir = System.getProperty("compose.application.resources.dir")
            val paths = mutableListOf(
                File("libs/vpn-helper"),
                File("desktop/libs/vpn-helper")
            )
            resourceDir?.let { paths.add(0, File(it, "vpn-helper")) }

            val helperFile = paths.find { it.exists() } ?: return@withContext Result.failure(Exception("VPN Helper not found in any of: $paths"))
            val helperPath = helperFile.absolutePath

            // Ensure permissions
            ensureHelperPermissions(helperFile)

            // Check if setuid bit is set (simplistic check: is owner root and has 's' in permissions)
            // If it's owned by root and has setuid, we don't need pkexec
            val isSetUid = try {
                val output = ProcessBuilder("ls", "-l", helperPath).start().inputStream.bufferedReader().readText()
                output.startsWith("-rws") || output.startsWith("-r-s")
            } catch (_: Exception) { false }

            // Cleanup interface if it exists
            println("Cleaning up interface $iface...")
            try {
                if (isSetUid) {
                    ProcessBuilder(helperPath, "--cleanup", iface).start().waitFor()
                } else {
                    ProcessBuilder("pkexec", helperPath, "--cleanup", iface).start().waitFor()
                }
            } catch (_: Exception) {}

            println("Starting VPN helper from $helperPath...")
            
            val cmd = if (isSetUid) {
                listOf(helperPath, iface, localIp, serverIp)
            } else {
                listOf("pkexec", helperPath, iface, localIp, serverIp)
            }

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val writer = OutputStreamWriter(process.outputStream)
            writer.write(config)
            writer.write("\n\n")
            writer.flush()

            val reader = process.inputStream.bufferedReader()
            var isConnected = false

            // Wait for helper to signal success with timeout (increased to 60s for password prompt)
            val result = withTimeoutOrNull(60000) {
                while (true) {
                    val line = reader.readLine() ?: break
                    println("Helper: $line")
                    if (line == "CONNECTED") {
                        isConnected = true
                        break
                    }
                    if (line.startsWith("ERROR:")) {
                        return@withTimeoutOrNull Result.failure<Unit>(Exception(line))
                    }
                }
                null
            }

            if (result != null) {
                process.destroy()
                return@withContext result
            }

            if (isConnected) {
                helperProcess = process
                Result.success(Unit)
            } else {
                process.destroy()
                Result.failure(Exception("Helper connection timeout or premature exit"))
            }
        } catch (e: Exception) {
            println("Failed to start helper: ${e.message}")
            Result.failure(e)
        }
    }

    private fun ensureHelperPermissions(helperFile: File) {
        try {
            val path = helperFile.absolutePath
            val output = ProcessBuilder("ls", "-l", path).start().inputStream.bufferedReader().readText()
            val isSetUid = output.startsWith("-rws") || output.startsWith("-r-s")
            
            if (!isSetUid) {
                println("Setting up persistent root permissions for vpn-helper (one-time elevation)...")
                // Use pkexec to set setuid bit once
                val pb = ProcessBuilder(
                    "pkexec", "sh", "-c", 
                    "chown root:root \"$path\" && chmod u+s \"$path\""
                )
                val proc = pb.start()
                if (proc.waitFor() == 0) {
                    println("Successfully enabled persistent root permissions for vpn-helper")
                } else {
                    println("Failed to set persistent permissions, will continue using pkexec for every connection")
                }
            }
        } catch (e: Exception) {
            println("Error checking/setting helper permissions: ${e.message}")
        }
    }

    fun disconnect(): Result<Unit> {
        helperProcess?.let {
            println("Disconnecting VPN...")
            
            try {
                // Closing stdin triggers cleanup in the helper
                it.outputStream.close()
            } catch (_: Exception) {}

            try {
                if (!it.waitFor(5, TimeUnit.SECONDS)) {
                    it.destroyForcibly()
                }
            } catch (e: Exception) {
                it.destroyForcibly()
            }
            helperProcess = null
            println("Disconnected.")
            return Result.success(Unit)
        }
        return Result.failure(Exception("Not connected"))
    }
}
