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

package ru.protonmod.next.desktop.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.data.network.PhysicalServer

/**
 * Desktop equivalent of Android's SessionEntity
 */
data class DesktopSessionEntity(
    val id: Int = 1,
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val userId: String,
    val userTier: Int = 0,
    val wgPrivateKey: String? = null,
    val wgPublicKeyPem: String? = null,
    val wgCertificate: String? = null
)

/**
 * Desktop equivalent of Android's ServerEntity
 */
data class DesktopServerEntity(
    val id: String,
    val name: String,
    val city: String,
    val exitCountry: String,
    val tier: Int,
    val features: Int,
    val averageLoad: Int = 0,
    val physicalServersJson: String
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDomain(server: LogicalServer): DesktopServerEntity {
            val sanitizedName = server.name.takeIf { !it.equals("null", ignoreCase = true) } ?: ""
            val sanitizedCity = server.city.takeIf { !it.equals("null", ignoreCase = true) } ?: ""
            val sanitizedExitCountry = server.exitCountry.takeIf { !it.equals("null", ignoreCase = true) } ?: ""

            val sanitizedPhysicalServers = server.servers.map { physical ->
                physical.copy(
                    domain = physical.domain.takeIf { !it.equals("null", ignoreCase = true) } ?: ""
                )
            }

            return DesktopServerEntity(
                id = server.id,
                name = sanitizedName,
                city = sanitizedCity,
                exitCountry = sanitizedExitCountry,
                tier = server.tier,
                features = server.features,
                averageLoad = server.averageLoad,
                physicalServersJson = json.encodeToString(sanitizedPhysicalServers)
            )
        }

        fun toDomain(entity: DesktopServerEntity): LogicalServer {
            return LogicalServer(
                id = entity.id,
                name = entity.name,
                city = entity.city,
                exitCountry = entity.exitCountry,
                entryCountry = entity.exitCountry,
                tier = entity.tier,
                features = entity.features,
                servers = try {
                    json.decodeFromString<List<PhysicalServer>>(entity.physicalServersJson)
                } catch (e: Exception) {
                    emptyList()
                },
                averageLoad = entity.averageLoad
            )
        }
    }
}

/**
 * Desktop equivalent of Android's ServersCacheEntity
 */
data class DesktopServersCacheEntity(
    val id: Int = 1,
    val cachedAt: Long,
    val expiresAt: Long,
    val lastModified: String? = null
)

/**
 * Desktop equivalent of Android's RecentConnectionEntity
 */
data class DesktopRecentConnectionEntity(
    val serverId: String,
    val serverName: String,
    val city: String,
    val country: String,
    val lastConnectedAt: Long
)

/**
 * Certificate state management
 */
sealed class CertificateState {
    data object Valid : CertificateState()
    data class ExpiringSoon(val hoursRemaining: Int) : CertificateState()
    data object Expired : CertificateState()
    data class RefreshFailed(val error: String, val isFullyExpired: Boolean) : CertificateState()
    data object Refreshing : CertificateState()
}
