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
data class LoadsResponse(
    @SerialName("Code") val code: Int? = null,
    @SerialName("LogicalServers") val loads: List<ServerLoad> = emptyList()
)

@Serializable
data class ServerLoad(
    @SerialName("ID") val id: String, // Logical or physical server ID
    @SerialName("Load") val load: Int, // Load in percent (0-100)
    @SerialName("Status") val status: Int? = null
)
