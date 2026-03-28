package ru.protonmod.next.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DesktopLoginViewModel(
    private val authClient: DesktopAuthClient,
    private val vpnClient: DesktopVpnClient
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val _uiState = MutableStateFlow<DesktopLoginUiState>(DesktopLoginUiState.Idle)
    val uiState: StateFlow<DesktopLoginUiState> = _uiState.asStateFlow()

    private val _servers = MutableStateFlow<List<ServerEntry>>(emptyList())
    val servers: StateFlow<List<ServerEntry>> = _servers.asStateFlow()

    private val _recentConnections = MutableStateFlow<List<ServerEntry>>(emptyList())
    val recentConnections: StateFlow<List<ServerEntry>> = _recentConnections.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectedServer = MutableStateFlow<ServerEntry?>(null)
    val connectedServer: StateFlow<ServerEntry?> = _connectedServer.asStateFlow()

    fun login(username: String, passwordRaw: String, captchaToken: String? = null) {
        scope.launch {
            _uiState.value = DesktopLoginUiState.Loading
            val result = authClient.login(username, passwordRaw, captchaToken)
            result.onSuccess { response ->
                val accessToken = response.accessToken.orEmpty()
                val sessionId = response.sessionId.orEmpty()
                
                // Perform one-time VPN setup (key registration) after login
                val setupResult = vpnClient.setupVpn(accessToken, sessionId)
                if (setupResult.isSuccess) {
                    _uiState.value = DesktopLoginUiState.Success(accessToken, sessionId)
                    loadServers(accessToken, sessionId)
                } else {
                    _uiState.value = DesktopLoginUiState.Error("VPN Setup failed: ${setupResult.exceptionOrNull()?.message}")
                }
            }.onFailure { error ->
                handleLoginFailure(error)
            }
        }
    }

    fun loginAnonymous(captchaToken: String? = null) {
        scope.launch {
            _uiState.value = DesktopLoginUiState.Loading
            val result = authClient.loginAnonymous(captchaToken)
            result.onSuccess { response ->
                val accessToken = response.accessToken.orEmpty()
                val sessionId = response.sessionId.orEmpty()
                
                // Perform one-time VPN setup (key registration) after guest login
                val setupResult = vpnClient.setupVpn(accessToken, sessionId)
                if (setupResult.isSuccess) {
                    _uiState.value = DesktopLoginUiState.Success(accessToken, sessionId)
                    loadServers(accessToken, sessionId)
                } else {
                    _uiState.value = DesktopLoginUiState.Error("VPN Setup failed: ${setupResult.exceptionOrNull()?.message}")
                }
            }.onFailure { error ->
                handleLoginFailure(error)
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

    fun connectToServer(server: ServerEntry) {
        val state = _uiState.value as? DesktopLoginUiState.Success ?: return
        
        scope.launch {
            _isConnecting.value = true
            val result = vpnClient.connect(state.accessToken, state.sessionId, server)
            _isConnecting.value = false
            
            result.onSuccess {
                _connectedServer.value = server
                val currentRecent = _recentConnections.value.toMutableList()
                currentRecent.removeIf { it.id == server.id }
                currentRecent.add(0, server)
                _recentConnections.value = currentRecent.take(5)
            }.onFailure { error ->
                _uiState.value = DesktopLoginUiState.Error("VPN connection failed: ${error.localizedMessage}")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            vpnClient.disconnect()
            _connectedServer.value = null
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
