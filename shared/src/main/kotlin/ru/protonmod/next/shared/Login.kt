package ru.protonmod.next.shared

import kotlinx.coroutines.delay

sealed interface LoginResult {
    data class Success(val token: String) : LoginResult
    data class Error(val message: String) : LoginResult
}

suspend fun guestLogin(email: String, password: String): LoginResult {
    // Minimal stub for desktop testing.
    // Replace this with a real API call when you know the endpoint and request format.
    delay(800)

    if (email.isBlank() || password.isBlank()) {
        return LoginResult.Error("Email and password must not be empty")
    }

    // Fake token to show login succeeded.
    return LoginResult.Success("guest-token-${email.hashCode()}")
}
