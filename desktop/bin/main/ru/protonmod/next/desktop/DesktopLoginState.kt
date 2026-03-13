package ru.protonmod.next.desktop

sealed class DesktopLoginUiState {
    object Idle : DesktopLoginUiState()
    object Loading : DesktopLoginUiState()
    data class RequiresCaptcha(
        val webUrl: String,
        val sessionId: String?,
        val captchaToken: String
    ) : DesktopLoginUiState()
    data class Success(
        val accessToken: String,
        val sessionId: String
    ) : DesktopLoginUiState()
    data class Error(val message: String) : DesktopLoginUiState()
}
