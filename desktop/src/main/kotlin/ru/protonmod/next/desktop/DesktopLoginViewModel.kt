package ru.protonmod.next.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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

    fun loginAnonymous(captchaToken: String? = null) {
        scope.launch {
            _uiState.value = DesktopLoginUiState.Loading
            val result = authClient.loginAnonymous(captchaToken)
            result.onSuccess { response ->
                val accessToken = response.accessToken.orEmpty()
                val sessionId = response.sessionId.orEmpty()
                _uiState.value = DesktopLoginUiState.Success(accessToken, sessionId)
                loadServers(accessToken, sessionId)
            }.onFailure { error ->
                when (error) {
                    is CaptchaRequiredException -> {
                        _uiState.value = DesktopLoginUiState.RequiresCaptcha(
                            webUrl = error.webUrl,
                            sessionId = error.sessionId,
                            captchaToken = error.token
                        )
                    }
                    else -> {
                        _uiState.value = DesktopLoginUiState.Error(error.localizedMessage ?: "Guest login failed")
                    }
                }
            }
        }
    }

    fun retryWithCaptcha(captchaToken: String) {
        loginAnonymous(captchaToken)
    }

    private fun loadServers(accessToken: String, sessionId: String) {
        scope.launch {
            // Hardcode tier 0 for guest login
            val result = vpnClient.getServers(accessToken, sessionId, userTier = 0)
            result.onSuccess { list ->
                _servers.value = list
            }
        }
    }

    fun connectToServer(server: ServerEntry) {
        // TODO: Implement real connection logic with Go libraries
        // For now, just add to recent
        val currentRecent = _recentConnections.value.toMutableList()
        currentRecent.removeIf { it.id == server.id }
        currentRecent.add(0, server)
        _recentConnections.value = currentRecent.take(5)
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
