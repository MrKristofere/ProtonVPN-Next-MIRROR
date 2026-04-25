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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.desktop.data.local.CertificateState
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.data.local.DesktopRecentConnectionEntity
import ru.protonmod.next.desktop.data.local.DesktopSessionEntity
import ru.protonmod.next.desktop.monitoring.DesktopSentryManager
import ru.protonmod.next.desktop.data.repository.DesktopCertificateManager
import ru.protonmod.next.desktop.data.repository.DesktopVpnRepository

class DesktopLoginViewModel(
    private val authClient: DesktopAuthClient,
    private val vpnClient: DesktopVpnClient,
    private val database: DesktopDatabase,
    private val vpnRepository: DesktopVpnRepository,
    private val certificateManager: DesktopCertificateManager,
    private val sentryManager: DesktopSentryManager? = null
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    val certificateState = certificateManager.certState

    private val _uiState = MutableStateFlow<DesktopLoginUiState>(DesktopLoginUiState.Idle)
    val uiState: StateFlow<DesktopLoginUiState> = _uiState.asStateFlow()

    private val _servers = MutableStateFlow<List<LogicalServer>>(emptyList())
    val servers: StateFlow<List<LogicalServer>> = _servers.asStateFlow()

    private val _recentConnections = MutableStateFlow<List<LogicalServer>>(emptyList())
    val recentConnections: StateFlow<List<LogicalServer>> = _recentConnections.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectedServer = MutableStateFlow<LogicalServer?>(null)
    val connectedServer: StateFlow<LogicalServer?> = _connectedServer.asStateFlow()

    init {
        // Load recent connections on init
        loadRecentConnections()
    }

    private fun loadRecentConnections() {
        scope.launch {
            val entities = vpnRepository.getRecentConnections()
            
            // Immediately map entities to LogicalServer even if servers list is not yet loaded
            // This provides immediate visual feedback on recent servers
            val initialRecent = entities.map { r ->
                LogicalServer(
                    id = r.serverId,
                    name = r.serverName,
                    city = r.city,
                    entryCountry = "",
                    exitCountry = r.country,
                    tier = 0,
                    features = 0,
                    servers = emptyList(),
                    averageLoad = 0
                )
            }
            _recentConnections.value = initialRecent

            // Refine with full LogicalServer (including physical server/load) once servers arrive
            _servers.collect { serverList ->
                if (serverList.isNotEmpty()) {
                    val refined = entities.mapNotNull { r ->
                        serverList.find { it.id == r.serverId }
                    }
                    _recentConnections.value = refined
                }
            }
        }
    }

    fun login(username: String, passwordRaw: String, captchaToken: String? = null) {
        val startTime = System.currentTimeMillis()
        scope.launch {
            _uiState.value = DesktopLoginUiState.Loading
            sentryManager?.trackLoginAttempt()
            
            val result = authClient.login(username, passwordRaw, captchaToken)
            result.onSuccess { response ->
                val duration = System.currentTimeMillis() - startTime
                sentryManager?.trackLoginSuccess(duration)
                
                val accessToken = response.accessToken.orEmpty()
                val sessionId = response.sessionId.orEmpty()
                
                println("DesktopLoginViewModel: Login successful. UID: $sessionId")
                
                if (sessionId.isEmpty()) {
                    println("DesktopLoginViewModel: ERROR: Received empty UID from API!")
                }

                // Save session to database
                database.saveSession(DesktopSessionEntity(
                    accessToken = accessToken,
                    refreshToken = response.refreshToken.orEmpty(),
                    sessionId = sessionId,
                    userId = response.userId.orEmpty(),
                    userTier = 0 // Default
                ))

                sentryManager?.setUserContext(username)
                
                // Try perform one-time VPN setup (key registration) after login
                // We proceed even if it fails, as CertificateManager will retry or user can manual refresh
                vpnClient.setupVpn()
                
                _uiState.value = DesktopLoginUiState.Success(accessToken, sessionId)
                loadServers(accessToken, sessionId)
            }.onFailure { error ->
                sentryManager?.trackLoginError(error.message ?: "Unknown login error")
                handleLoginFailure(error)
            }
        }
    }

    fun loginAnonymous(captchaToken: String? = null) {
        val startTime = System.currentTimeMillis()
        scope.launch {
            _uiState.value = DesktopLoginUiState.Loading
            sentryManager?.trackLoginAttempt()
            
            val result = authClient.loginAnonymous(captchaToken)
            result.onSuccess { response ->
                val duration = System.currentTimeMillis() - startTime
                sentryManager?.trackLoginSuccess(duration)
                
                val accessToken = response.accessToken.orEmpty()
                val sessionId = response.sessionId.orEmpty()
                
                println("DesktopLoginViewModel: Guest login successful. UID: $sessionId")

                if (sessionId.isEmpty()) {
                    println("DesktopLoginViewModel: ERROR: Received empty UID from API for guest!")
                }

                // Save anonymous session to database
                database.saveSession(DesktopSessionEntity(
                    accessToken = accessToken,
                    refreshToken = response.refreshToken.orEmpty(),
                    sessionId = sessionId,
                    userId = response.userId.orEmpty(),
                    userTier = 0
                ))

                sentryManager?.setUserContext("anonymous")
                
                // Try perform one-time VPN setup (key registration) after guest login
                vpnClient.setupVpn()
                
                _uiState.value = DesktopLoginUiState.Success(accessToken, sessionId)
                loadServers(accessToken, sessionId)
            }.onFailure { error ->
                sentryManager?.trackLoginError(error.message ?: "Unknown guest login error")
                handleLoginFailure(error)
            }
        }
    }

    fun restoreSession(accessToken: String, sessionId: String) {
        scope.launch {
            _uiState.value = DesktopLoginUiState.Loading
            try {
                // Verify session by loading servers
                loadServers(accessToken, sessionId)
                _uiState.value = DesktopLoginUiState.Success(accessToken, sessionId)
            } catch (e: Exception) {
                _uiState.value = DesktopLoginUiState.Idle
            }
        }
    }

    private fun handleLoginFailure(error: Throwable) {
        when (error) {
            is CaptchaRequiredException -> {
                _uiState.value = DesktopLoginUiState.RequiresCaptcha(
                    webUrl = error.webUrl,
                    sessionId = error.sessionId,
                    captchaToken = error.token
                )
            }
            else -> {
                _uiState.value = DesktopLoginUiState.Error(error.localizedMessage ?: "Login failed")
            }
        }
    }

    fun retryWithCaptcha(captchaToken: String) {
        loginAnonymous(captchaToken)
    }

    private fun loadServers(accessToken: String, sessionId: String) {
        scope.launch {
            val result = vpnClient.getServers(accessToken, sessionId, userTier = 0)
            result.onSuccess { list ->
                _servers.value = list
            }
        }
    }

    fun connectToServer(server: LogicalServer) {
        if (_uiState.value !is DesktopLoginUiState.Success) return
        
        scope.launch {
            _isConnecting.value = true
            val result = vpnClient.connect(server)
            _isConnecting.value = false
            
            result.onSuccess {
                // Trigger background server list refresh after connection
                // This follows the 10-second delay logic specified in requirements
                val session = database.getSession()
                if (session != null) {
                    vpnRepository.refreshServersAfterConnection(session.accessToken, session.sessionId, session.userTier)
                }

                _connectedServer.value = server
                // Add to recent connections
                vpnRepository.addRecentConnection(
                    DesktopRecentConnectionEntity(
                        serverId = server.id,
                        serverName = server.name,
                        city = server.city,
                        country = server.exitCountry,
                        lastConnectedAt = System.currentTimeMillis()
                    )
                )
                // Trigger reload of recent list
                loadRecentConnections()
            }.onFailure { error ->
                _uiState.value = DesktopLoginUiState.Error("VPN connection failed: ${error.localizedMessage}")
            }
        }
    }

    fun quickConnect(strategy: String, targetId: String? = null) {
        if (_uiState.value !is DesktopLoginUiState.Success) return
        
        scope.launch {
            val serverList = _servers.value
            if (serverList.isEmpty()) return@launch

            val targetServer = when (strategy) {
                "recent" -> {
                    _recentConnections.value.firstOrNull() ?: serverList.minByOrNull { it.averageLoad }
                }
                "server" -> {
                    serverList.find { it.id == targetId } ?: serverList.minByOrNull { it.averageLoad }
                }
                else -> {
                    // Default: "fastest"
                    serverList.minByOrNull { it.averageLoad }
                }
            }

            targetServer?.let { connectToServer(it) }
        }
    }

    fun disconnect() {
        scope.launch {
            vpnClient.disconnect()
            _connectedServer.value = null
        }
    }

    fun logout() {
        scope.launch {
            // Disconnect VPN first
            vpnClient.disconnect()
            _connectedServer.value = null
            
            // Clear secure session from database
            database.clearSession()
            
            // Reset UI state
            _uiState.value = DesktopLoginUiState.Idle
            _servers.value = emptyList()
        }
    }

    fun refreshCertificate() {
        scope.launch {
            certificateManager.forceRefreshCertificate()
        }
    }

    fun clearError() {
        if (_uiState.value is DesktopLoginUiState.Error) {
            _uiState.value = DesktopLoginUiState.Idle
        }
    }

    fun dispose() {
        job.cancel()
    }
}
