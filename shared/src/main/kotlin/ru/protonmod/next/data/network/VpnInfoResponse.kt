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

package ru.protonmod.next.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VpnInfoResponse(
    @SerialName("Code") val code: Int,
    @SerialName("VPN") val vpnInfo: VpnInfo? = null,
    @SerialName("Subscribed") val subscribed: Int? = null,
    @SerialName("Services") val services: Int? = null,
    @SerialName("Delinquent") val delinquent: Int? = null,
    @SerialName("Credit") val credit: Int? = null
)

@Serializable
data class VpnInfo(
    @SerialName("Status") val status: Int? = null,
    @SerialName("ExpirationTime") val expirationTime: Long? = null,
    @SerialName("MaxTier") val maxTier: Int? = null,
    @SerialName("MaxConnect") val maxConnect: Int? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("Password") val password: String? = null,
    @SerialName("PlanName") val planName: String? = null,
    @SerialName("PlanDisplayName") val planDisplayName: String? = null,
    @SerialName("GroupID") val groupId: String? = null
)
