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

package ru.protonmod.next.desktop.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.protonmod.next.desktop.monitoring.DesktopSentryManager
import java.io.File
import java.net.InetAddress

/**
 * Split Tunneling Configuration for Desktop
 */
@Serializable
data class SplitTunnelingConfig(
    val enabled: Boolean = false,
    val mode: String = "exclude",  // "exclude" or "include"
    val excludedApps: Set<String> = emptySet(),  // Desktop: executable paths
    val excludedIps: Set<String> = emptySet(),   // CIDR notation
    val excludedDomains: Set<String> = emptySet()
)

/**
 * Desktop Split Tunneling Manager
 * Manages split tunneling configuration including apps, IPs, and domains.
 * 
 * For Desktop, "apps" are executable paths instead of package names.
 * Supports exclude and include modes.
 */
class DesktopSplitTunnelingManager(
    private val configFile: File = File("split_tunneling.json"),
    private val sentryManager: DesktopSentryManager = DesktopSentryManager()
) {
    
    companion object {
        private const val TAG = "SplitTunneling"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _config = MutableStateFlow(loadConfiguration())
    val config: StateFlow<SplitTunnelingConfig> = _config.asStateFlow()

    init {
        sentryManager.debugLog("Split tunneling initialized", mapOf(
            "config_file" to configFile.absolutePath,
            "enabled" to _config.value.enabled.toString()
        ))
    }

    // ===== Configuration Management =====

    /**
     * Load configuration from file
     */
    private fun loadConfiguration(): SplitTunnelingConfig {
        return try {
            if (configFile.exists()) {
                val config = json.decodeFromString<SplitTunnelingConfig>(configFile.readText())
                println("$TAG: Loaded configuration - enabled=${config.enabled}, mode=${config.mode}")
                config
            } else {
                println("$TAG: Configuration file not found, using defaults")
                SplitTunnelingConfig()
            }
        } catch (e: Exception) {
            println("$TAG: Error loading configuration: ${e.message}")
            sentryManager.captureException(e, "SplitTunneling load configuration")
            SplitTunnelingConfig()
        }
    }

    /**
     * Save configuration to file
     */
    private fun saveConfiguration(config: SplitTunnelingConfig) {
        try {
            configFile.writeText(json.encodeToString(config))
            _config.value = config
            println("$TAG: Configuration saved - enabled=${config.enabled}, apps=${config.excludedApps.size}, ips=${config.excludedIps.size}, domains=${config.excludedDomains.size}")
            
            sentryManager.trackSplitTunnelingConfigured(
                config.enabled,
                config.mode,
                config.excludedApps.size,
                config.excludedIps.size,
                config.excludedDomains.size
            )
        } catch (e: Exception) {
            println("$TAG: Error saving configuration: ${e.message}")
            sentryManager.captureException(e, "SplitTunneling save configuration")
        }
    }

    // ===== Enable/Disable +====

    /**
     * Enable or disable split tunneling
     */
    fun setEnabled(enabled: Boolean) {
        val updated = _config.value.copy(enabled = enabled)
        saveConfiguration(updated)
        println("$TAG: Split tunneling ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Set the split tunneling mode
     */
    fun setMode(mode: String) {
        if (mode !in listOf("exclude", "include")) {
            println("$TAG: Invalid mode: $mode")
            return
        }
        val updated = _config.value.copy(mode = mode)
        saveConfiguration(updated)
        println("$TAG: Mode changed to $mode")
    }

    // ===== App Management =====

    /**
     * Get all excluded application paths
     */
    fun getExcludedApps(): Set<String> = _config.value.excludedApps

    /**
     * Add application to exclusion list
     */
    fun addExcludedApp(appPath: String) {
        if (appPath.isBlank()) {
            println("$TAG: Cannot add empty app path")
            return
        }
        
        val updated = _config.value.copy(
            excludedApps = _config.value.excludedApps + appPath
        )
        saveConfiguration(updated)
        println("$TAG: Added excluded app: $appPath")
        sentryManager.trackAppExclusionChanged(appPath, true)
    }

    /**
     * Remove application from exclusion list
     */
    fun removeExcludedApp(appPath: String) {
        val updated = _config.value.copy(
            excludedApps = _config.value.excludedApps - appPath
        )
        saveConfiguration(updated)
        println("$TAG: Removed excluded app: $appPath")
        sentryManager.trackAppExclusionChanged(appPath, false)
    }

    /**
     * Batch update excluded apps
     */
    fun setExcludedApps(apps: Set<String>) {
        val updated = _config.value.copy(excludedApps = apps)
        saveConfiguration(updated)
        println("$TAG: Updated excluded apps count: ${apps.size}")
    }

    /**
     * Clear all excluded apps
     */
    fun clearExcludedApps() {
        val updated = _config.value.copy(excludedApps = emptySet())
        saveConfiguration(updated)
        println("$TAG: Cleared all excluded apps")
    }

    // ===== IP Management =====

    /**
     * Get all excluded IP addresses/ranges
     */
    fun getExcludedIps(): Set<String> = _config.value.excludedIps

    /**
     * Validate IP address or CIDR notation
     */
    private fun isValidIp(ip: String): Boolean {
        return try {
            val trimmed = ip.trim()
            
            // Handle CIDR notation
            if (trimmed.contains("/")) {
                val parts = trimmed.split("/")
                if (parts.size != 2) return false
                
                val address = parts[0]
                val prefix = parts[1].toIntOrNull() ?: return false
                
                // Validate IP address
                InetAddress.getByName(address)
                
                // Validate prefix length
                if (address.contains(":")) {
                    prefix in 0..128  // IPv6
                } else {
                    prefix in 0..32   // IPv4
                }
            } else {
                // Single IP address
                InetAddress.getByName(trimmed)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Normalize IP address notation
     */
    private fun normalizeIp(ip: String): String {
        return ip.trim().lowercase()
    }

    /**
     * Add IP address/range to exclusion list
     */
    fun addExcludedIp(ip: String): Boolean {
        if (!isValidIp(ip)) {
            println("$TAG: Invalid IP address: $ip")
            return false
        }
        
        val normalized = normalizeIp(ip)
        if (normalized in _config.value.excludedIps) {
            println("$TAG: IP already excluded: $ip")
            return false
        }
        
        val updated = _config.value.copy(
            excludedIps = _config.value.excludedIps + normalized
        )
        saveConfiguration(updated)
        println("$TAG: Added excluded IP: $ip")
        return true
    }

    /**
     * Remove IP address/range from exclusion list
     */
    fun removeExcludedIp(ip: String) {
        val normalized = normalizeIp(ip)
        val updated = _config.value.copy(
            excludedIps = _config.value.excludedIps - normalized
        )
        saveConfiguration(updated)
        println("$TAG: Removed excluded IP: $ip")
    }

    /**
     * Batch update excluded IPs
     */
    fun setExcludedIps(ips: Set<String>) {
        val validIps = ips.filter { isValidIp(it) }.map { normalizeIp(it) }.toSet()
        if (validIps.size != ips.size) {
            println("$TAG: Some IPs were invalid and filtered out")
        }
        
        val updated = _config.value.copy(excludedIps = validIps)
        saveConfiguration(updated)
        println("$TAG: Updated excluded IPs count: ${validIps.size}")
    }

    /**
     * Clear all excluded IPs
     */
    fun clearExcludedIps() {
        val updated = _config.value.copy(excludedIps = emptySet())
        saveConfiguration(updated)
        println("$TAG: Cleared all excluded IPs")
    }

    // ===== Domain Management =====

    /**
     * Get all excluded domains
     */
    fun getExcludedDomains(): Set<String> = _config.value.excludedDomains

    /**
     * Validate domain name
     */
    private fun isValidDomain(domain: String): Boolean {
        val trimmed = domain.trim().lowercase()
        
        // Simple domain validation
        if (trimmed.isEmpty()) return false
        if (trimmed.length > 255) return false
        if (trimmed.startsWith(".") || trimmed.endsWith(".")) return false
        
        // Check for valid domain pattern
        val domainRegex = Regex("^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$")
        if (!domainRegex.matches(trimmed)) return false
        
        return true
    }

    /**
     * Add domain to exclusion list
     */
    fun addExcludedDomain(domain: String): Boolean {
        if (!isValidDomain(domain)) {
            println("$TAG: Invalid domain: $domain")
            return false
        }
        
        val normalized = domain.trim().lowercase()
        if (normalized in _config.value.excludedDomains) {
            println("$TAG: Domain already excluded: $domain")
            return false
        }
        
        val updated = _config.value.copy(
            excludedDomains = _config.value.excludedDomains + normalized
        )
        saveConfiguration(updated)
        println("$TAG: Added excluded domain: $domain")
        return true
    }

    /**
     * Remove domain from exclusion list
     */
    fun removeExcludedDomain(domain: String) {
        val normalized = domain.trim().lowercase()
        val updated = _config.value.copy(
            excludedDomains = _config.value.excludedDomains - normalized
        )
        saveConfiguration(updated)
        println("$TAG: Removed excluded domain: $domain")
    }

    /**
     * Batch update excluded domains
     */
    fun setExcludedDomains(domains: Set<String>) {
        val validDomains = domains.filter { isValidDomain(it) }.map { it.trim().lowercase() }.toSet()
        if (validDomains.size != domains.size) {
            println("$TAG: Some domains were invalid and filtered out")
        }
        
        val updated = _config.value.copy(excludedDomains = validDomains)
        saveConfiguration(updated)
        println("$TAG: Updated excluded domains count: ${validDomains.size}")
    }

    /**
     * Clear all excluded domains
     */
    fun clearExcludedDomains() {
        val updated = _config.value.copy(excludedDomains = emptySet())
        saveConfiguration(updated)
        println("$TAG: Cleared all excluded domains")
    }

    /**
     * Resolve domains to IP addresses for VPN configuration
     */
    fun resolveDomensToIps(): Set<String> {
        val resolvedIps = mutableSetOf<String>()
        
        for (domain in _config.value.excludedDomains) {
            try {
                val addresses = InetAddress.getAllByName(domain)
                addresses.forEach { addr ->
                    val ip = addr.hostAddress
                    if (ip != null) {
                        val ipWithPrefix = if (ip.contains(":")) "$ip/128" else "$ip/32"
                        resolvedIps.add(ipWithPrefix)
                        println("$TAG: Resolved domain $domain to $ip")
                    }
                }
            } catch (e: Exception) {
                println("$TAG: Failed to resolve domain $domain: ${e.message}")
                sentryManager.trackDnsResolutionError(domain, e.message ?: "Unknown")
            }
        }
        
        return resolvedIps
    }

    // ===== Status =====

    /**
     * Get complete configuration summary
     */
    fun getConfigurationSummary(): String {
        val cfg = _config.value
        return """
            Split Tunneling Configuration:
            - Enabled: ${cfg.enabled}
            - Mode: ${cfg.mode}
            - Excluded Apps: ${cfg.excludedApps.size}
            - Excluded IPs: ${cfg.excludedIps.size}
            - Excluded Domains: ${cfg.excludedDomains.size}
        """.trimIndent()
    }

    /**
     * Check if split tunneling is fully configured
     */
    fun isFullyConfigured(): Boolean {
        if (!_config.value.enabled) return false
        
        val hasExclusions = _config.value.excludedApps.isNotEmpty() ||
                           _config.value.excludedIps.isNotEmpty() ||
                           _config.value.excludedDomains.isNotEmpty()
        
        return hasExclusions
    }
}
