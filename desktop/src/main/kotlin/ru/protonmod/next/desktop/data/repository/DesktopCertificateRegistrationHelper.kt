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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.protonmod.next.data.network.CreateCertificateRequest
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.data.local.DesktopSessionEntity
import ru.protonmod.next.desktop.native.VpnNative
import java.util.Base64

/**
 * Desktop Certificate Registration Helper
 * 
 * Simplifies certificate registration flow for authentication
 * Handles key generation, registration with Proton API, and local persistence
 * 
 * This can be used during login to automatically register WireGuard certificate:
 * 
 * ```kotlin
 * // After successful authentication
 * val helper = DesktopCertificateRegistrationHelper(database, vpnRepository)
 * val result = helper.registerNewCertificate(
 *     accessToken = accessToken,
 *     sessionId = sessionId,
 *     userTier = userTier
 * )
 * ```
 */
class DesktopCertificateRegistrationHelper(
    private val database: DesktopDatabase,
    private val vpnRepository: DesktopVpnRepository
) {
    companion object {
        private const val TAG = "DesktopCertRegHelper"
    }

    /**
     * Register new WireGuard certificate after authentication
     * 
     * Flow:
     * 1. Generate new WireGuard keypair via go-bridge
     * 2. Register public key with Proton API
     * 3. Store certificate and keys in local database
     * 4. Return certificate for VPN connection
     * 
     * @param accessToken User's access token
     * @param sessionId User's session ID
     * @param userTier User's VPN tier
     * @return Result containing certificate PEM or error
     */
    suspend fun registerNewCertificate(
        accessToken: String,
        sessionId: String,
        userTier: Int
    ): Result<String> {
        return try {
            println("$TAG: Generating new WireGuard keys via go-bridge...")
            
            // Generate keys using go-bridge
            val result = VpnNative.INSTANCE.GenerateWGKeys()
            if (result.startsWith("ERROR:")) {
                return Result.failure(Exception("Go-bridge error: $result"))
            }
            
            // Parse: X25519_PRIV_B64 ED25519_PUB_PEM_B64 X25519_PUB_B64
            val parts = result.split(" ")
            if (parts.size < 2) {
                return Result.failure(Exception("Invalid key generation response format"))
            }
            
            val privateKeyX25519 = parts[0]
            val publicKeyPemB64 = parts[1]
            val publicKeyPem = String(Base64.getDecoder().decode(publicKeyPemB64))
            
            println("$TAG: Keys generated successfully")
            println("$TAG: Registering public key with Proton API...")
            
            // Register with Proton API
            val certResponse = vpnRepository.registerWireGuardKey(
                accessToken = accessToken,
                sessionId = sessionId,
                publicKeyPem = publicKeyPem
            ).getOrNull()
            
            if (certResponse == null || certResponse.certificate == null) {
                return Result.failure(Exception("Failed to register certificate with Proton API"))
            }
            
            val certificate = certResponse.certificate!!
            println("$TAG: Certificate received from Proton API")
            
            // Store in database
            val session = database.getSession()
            if (session != null) {
                database.updateVpnKeys(
                    privateKey = privateKeyX25519,
                    publicKeyPem = publicKeyPem,
                    certificate = certificate
                )
                println("$TAG: Certificate and keys stored in database")
            } else {
                println("$TAG: Warning: No session found in database, skipping storage")
            }
            
            Result.success(certificate)
        } catch (e: Exception) {
            println("$TAG: Certificate registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Register new certificate and update session in a transaction-like operation
     * 
     * Useful when creating new session during login
     */
    suspend fun registerAndSaveSession(
        accessToken: String,
        refreshToken: String,
        sessionId: String,
        userId: String,
        userTier: Int
    ): Result<DesktopSessionEntity> {
        return try {
            // First register certificate
            val certificate = registerNewCertificate(
                accessToken = accessToken,
                sessionId = sessionId,
                userTier = userTier
            ).getOrNull()
                ?: return Result.failure(Exception("Certificate registration failed"))
            
            // Generate keys
            val result = VpnNative.INSTANCE.GenerateWGKeys()
            if (result.startsWith("ERROR:")) {
                return Result.failure(Exception("Go-bridge error: $result"))
            }
            
            val parts = result.split(" ")
            val privateKeyX25519 = parts[0]
            val publicKeyPemB64 = parts[1]
            val publicKeyPem = String(Base64.getDecoder().decode(publicKeyPemB64))
            
            // Create and save session
            val session = DesktopSessionEntity(
                accessToken = accessToken,
                refreshToken = refreshToken,
                sessionId = sessionId,
                userId = userId,
                userTier = userTier,
                wgPrivateKey = privateKeyX25519,
                wgPublicKeyPem = publicKeyPem,
                wgCertificate = certificate
            )
            
            database.saveSession(session)
            println("$TAG: Session saved successfully")
            
            Result.success(session)
        } catch (e: Exception) {
            println("$TAG: Failed to register and save session: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Refresh certificate for existing session
     * Generates new keys and registers new certificate
     */
    suspend fun refreshCertificateForSession(): Result<String> {
        return try {
            val session = database.getSession()
                ?: return Result.failure(Exception("No active session found"))
            
            println("$TAG: Refreshing certificate for user tier ${session.userTier}")
            registerNewCertificate(
                accessToken = session.accessToken,
                sessionId = session.sessionId,
                userTier = session.userTier
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Usage in DesktopLoginViewModel:
 * 
 * ```kotlin
 * private val certRegHelper by lazy {
 *     DesktopCertificateRegistrationHelper(database, vpnRepository)
 * }
 * 
 * suspend fun performLogin() {
 *     val authResult = authClient.login(username, password)
 *     if (authResult.isSuccess) {
 *         val loginResponse = authResult.getOrNull()!!
 *         
 *         // Register certificate and save session
 *         val sessionResult = certRegHelper.registerAndSaveSession(
 *             accessToken = loginResponse.accessToken,
 *             refreshToken = loginResponse.refreshToken,
 *             sessionId = loginResponse.sessionId,
 *             userId = loginResponse.userId,
 *             userTier = userTier
 *         )
 *         
 *         if (sessionResult.isSuccess) {
 *             // Start background server updates
 *             vpnRepository.startAutoUpdate()
 *             _uiState.value = DesktopLoginUiState.Success(...)
 *         } else {
 *             _uiState.value = DesktopLoginUiState.Error(...)
 *         }
 *     }
 * }
 * ```
 */
