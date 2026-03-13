package ru.protonmod.next.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("Code") val code: Int,
    @SerialName("AccessToken") val accessToken: String? = null,
    @SerialName("RefreshToken") val refreshToken: String? = null,
    @SerialName("UID") val sessionId: String? = null,
    @SerialName("UserID") val userId: String? = null,
    @SerialName("Scopes") val scopes: List<String> = emptyList()
)

@Serializable
data class ProtonErrorResponse(
    @SerialName("Code") val code: Int,
    @SerialName("Details") val details: ProtonErrorDetails? = null
)

@Serializable
data class ProtonErrorDetails(
    @SerialName("WebUrl") val webUrl: String? = null,
    @SerialName("HumanVerificationToken") val humanVerificationToken: String? = null
)

class CaptchaRequiredException(val webUrl: String, val token: String, val sessionId: String?) : Exception("Human Verification Required")

/**
 * Simple server representation used by the desktop UI.
 */
data class ServerEntry(
    val id: String,
    val name: String,
    val city: String,
    val country: String,
    val tier: Int
)
