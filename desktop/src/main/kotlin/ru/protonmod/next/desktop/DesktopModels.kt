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
