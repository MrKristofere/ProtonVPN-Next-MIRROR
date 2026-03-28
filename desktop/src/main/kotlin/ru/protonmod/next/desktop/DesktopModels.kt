package ru.protonmod.next.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.protonmod.next.data.network.PhysicalServer

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
    val tier: Int,
    val physicalServer: PhysicalServer? = null
)
