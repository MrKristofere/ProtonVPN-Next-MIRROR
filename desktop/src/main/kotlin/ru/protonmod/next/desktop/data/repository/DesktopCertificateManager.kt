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

package ru.protonmod.next.desktop.data.repository

import io.sentry.Sentry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.protonmod.next.data.network.CreateCertificateResponse
import ru.protonmod.next.desktop.data.local.CertificateState
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.native.VpnNative
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Desktop Certificate Manager
 * Handles WireGuard certificate lifecycle, state management, and proactive refresh
 * Mirrors Android AmneziaVpnManager certificate handling logic
 * 
 * Uses go-bridge (libgovpn.so) for key generation via VpnNative interface
 */
class DesktopCertificateManager(
    private val database: DesktopDatabase,
    private val vpnRepository: DesktopVpnRepository,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _certState = MutableStateFlow<CertificateState>(CertificateState.Valid)
    val certState: StateFlow<CertificateState> = _certState.asStateFlow()

    private val refreshMutex = Mutex()
    private var refreshJob: Job? = null

    companion object {
        private const val TAG = "DesktopCertManager"
        // Refresh certificate if expiring within 1 hour
        private const val REFRESH_THRESHOLD_MS = 60 * 60 * 1000L
        // Check certificate state every 2 hours if valid
        private const val PERIODIC_REFRESH_MS = 2 * 60 * 60 * 1000L
        // Start retrying after 1 minute
        private const val INITIAL_RETRY_DELAY_MS = 60 * 1000L
        // Maximum retry delay (10 minutes)
        private const val RETRY_DELAY_MS = 10 * 60 * 1000L
    }

    init {
        applicationScope.launch {
            delay(1500) // Staggered initialization
            val session = database.getSession()
            if (session != null) {
                updateCertificateState(session.wgCertificate)
                if (_certState.value !is CertificateState.Valid) {
                    checkAndRefreshCertificateProactively()
                }
            }
        }
    }

    /**
     * Update certificate state based on X.509 certificate expiry
     */
    private fun updateCertificateState(certPem: String?) {
        if (certPem.isNullOrEmpty()) {
            _certState.value = CertificateState.Expired
            return
        }
        try {
            val cf = CertificateFactory.getInstance("X.509")
            val x509 = cf.generateCertificate(ByteArrayInputStream(certPem.toByteArray())) as X509Certificate
            val now = System.currentTimeMillis()
            val expiry = x509.notAfter.time

            if (now >= expiry) {
                _certState.value = CertificateState.Expired
            } else if (expiry - now < REFRESH_THRESHOLD_MS) {
                val hours = ((expiry - now) / (3600 * 1000L)).toInt()
                _certState.value = CertificateState.ExpiringSoon(hours)
            } else {
                _certState.value = CertificateState.Valid
            }
        } catch (e: Exception) {
            println("$TAG: Error parsing certificate: ${e.message}")
            _certState.value = CertificateState.Expired
        }
    }

    /**
     * Generate VPN key pair using go-bridge native library
     * 
     * Calls VpnNative.GenerateWGKeys() which returns:
     * "X25519_PRIV_B64 ED25519_PUB_PEM_B64 X25519_PUB_B64"
     */
    private fun generateVpnKeyPair(): Pair<String, String> {
        return try {
            println("$TAG: Generating new VPN keys via go-bridge...")
            val result = VpnNative.INSTANCE.GenerateWGKeys()
            
            if (result.startsWith("ERROR:")) {
                throw Exception("Go-bridge error: $result")
            }
            
            // Format: X25519_PRIV_B64 ED25519_PUB_PEM_B64 X25519_PUB_B64
            val parts = result.split(" ")
            if (parts.size < 2) {
                throw Exception("Invalid key generation response format: $result")
            }
            
            val privateKeyX25519 = parts[0]
            val publicKeyPemB64 = parts[1]
            
            // Decode the base64-encoded PEM
            val publicKeyPem = String(Base64.getDecoder().decode(publicKeyPemB64))
            
            println("$TAG: VPN keys generated successfully via go-bridge")
            Pair(privateKeyX25519, publicKeyPem)
        } catch (e: Exception) {
            println("$TAG: Failed to generate VPN keypair: ${e.message}")
            throw e
        }
    }

    /**
     * Perform certificate refresh with key generation
     */
    private suspend fun performCertificateRefresh(force: Boolean = false): Result<String> = refreshMutex.withLock {
        val currentSession = database.getSession() ?: return Result.failure<String>(Exception("No session")).also {
            println("$TAG: Certificate refresh failed: No active session found in database")
        }
        updateCertificateState(currentSession.wgCertificate)

        if (!force && _certState.value is CertificateState.Valid) {
            println("$TAG: Certificate is still valid, skipping refresh")
            return Result.success(currentSession.wgCertificate ?: "")
        }

        val previousState = _certState.value
        _certState.value = CertificateState.Refreshing
        println("$TAG: Starting certificate refresh (force=$force, previous state: $previousState)")

        // Generate new VPN key pair via go-bridge
        val keyPair = try {
            generateVpnKeyPair()
        } catch (e: Exception) {
            println("$TAG: Failed to generate VPN keypair: ${e.message}")
            _certState.value = previousState
            return Result.failure(e)
        }

        val result = vpnRepository.registerWireGuardKey(
            accessToken = currentSession.accessToken,
            sessionId = currentSession.sessionId,
            publicKeyPem = keyPair.second
        )

        return if (result.isSuccess) {
            val response = result.getOrNull()
            val newCert = response?.certificate
            if (newCert != null) {
                println("$TAG: Successfully registered new WireGuard key and received certificate")

                // Metrics
                Sentry.metrics().count("desktop_cert_refresh_success", 1.0)

                database.updateVpnKeys(
                    privateKey = keyPair.first,
                    publicKeyPem = keyPair.second,
                    certificate = newCert
                )
                updateCertificateState(newCert)
                Result.success(newCert)
            } else {
                println("$TAG: Server returned success but certificate is null or empty")
                _certState.value = previousState
                Result.failure(Exception("Empty certificate in response"))
            }
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            println("$TAG: Failed to register WireGuard key with Proton API: $error")

            // Metrics
            Sentry.metrics().count("desktop_cert_refresh_error", 1.0)

            val isFullyExpired = previousState is CertificateState.Expired ||
                    (previousState is CertificateState.RefreshFailed && previousState.isFullyExpired)
            _certState.value = CertificateState.RefreshFailed(error, isFullyExpired)
            Result.failure(result.exceptionOrNull() ?: Exception(error))
        }
    }

    /**
     * Start proactive certificate refresh checks
     */
    fun checkAndRefreshCertificateProactively() {
        if (refreshJob?.isActive == true) return
        refreshJob = applicationScope.launch {
            var currentRetryDelay = INITIAL_RETRY_DELAY_MS
            while (isActive) {
                val session = database.getSession() ?: break
                updateCertificateState(session.wgCertificate)

                if (_certState.value is CertificateState.Valid) {
                    // All good, check again in 2 hours
                    delay(PERIODIC_REFRESH_MS)
                    currentRetryDelay = 5000L
                    continue
                }

                println("$TAG: Proactive refresh starting (cert state: ${_certState.value})")
                val result = performCertificateRefresh(force = false)

                if (result.isSuccess) {
                    currentRetryDelay = 5000L
                    delay(PERIODIC_REFRESH_MS)
                } else {
                    println("$TAG: Proactive refresh failed, retrying in ${currentRetryDelay}ms")
                    delay(currentRetryDelay)
                    currentRetryDelay = (currentRetryDelay * 2).coerceAtMost(RETRY_DELAY_MS)
                }
            }
        }
    }

    /**
     * Check if certificate is effectively expired
     */
    fun isEffectivelyExpired(): Boolean {
        val state = _certState.value
        return state is CertificateState.Expired || (state is CertificateState.RefreshFailed && state.isFullyExpired)
    }

    /**
     * Force refresh certificate immediately
     */
    suspend fun forceRefreshCertificate(): Result<String> {
        return performCertificateRefresh(force = true)
    }

    /**
     * Stop certificate refresh
     */
    fun stopCertificateRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }
}
