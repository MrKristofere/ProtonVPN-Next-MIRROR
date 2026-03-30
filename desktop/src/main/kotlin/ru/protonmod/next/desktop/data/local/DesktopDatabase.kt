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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.protonmod.next.desktop.data.security.DesktopCryptoManager
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Desktop SQLite Database Manager
 * Handles database initialization and operations for servers, sessions, and cache metadata
 */
class DesktopDatabase(private val dbPath: String = getDefaultDatabasePath()) {

    private val cryptoManager = DesktopCryptoManager()

    companion object {
        private fun getDefaultDatabasePath(): String {
            val appDataDir = when {
                System.getProperty("os.name").lowercase().contains("win") -> {
                    System.getenv("APPDATA") + File.separator + "ProtonVPN-Next"
                }
                System.getProperty("os.name").lowercase().contains("mac") -> {
                    File(System.getProperty("user.home"), "Library/Application Support/ProtonVPN-Next").absolutePath
                }
                else -> {
                    File(System.getProperty("user.home"), ".local/share/ProtonVPN-Next").absolutePath
                }
            }
            File(appDataDir).mkdirs()
            return File(appDataDir, "protonvpn.db").absolutePath
        }

        private const val TAG = "DesktopDatabase"
    }

    private var connection: Connection? = null

    init {
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            println("SQLite JDBC driver not found. Make sure sqlite-jdbc dependency is added.")
            throw e
        }
    }

    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            initializeSchema()
            println("Database connected: $dbPath")
        } catch (e: Exception) {
            println("Failed to connect to database: ${e.message}")
            throw e
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            connection?.close()
            connection = null
            println("Database disconnected")
        } catch (e: Exception) {
            println("Error disconnecting database: ${e.message}")
        }
    }

    private suspend fun initializeSchema() = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext

        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """CREATE TABLE IF NOT EXISTS session (
                    id INTEGER PRIMARY KEY,
                    access_token TEXT NOT NULL,
                    refresh_token TEXT NOT NULL,
                    session_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    user_tier INTEGER DEFAULT 0,
                    wg_private_key TEXT,
                    wg_public_key_pem TEXT,
                    wg_certificate TEXT
                )"""
            )

            statement.executeUpdate(
                """CREATE TABLE IF NOT EXISTS servers (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    city TEXT NOT NULL,
                    exit_country TEXT NOT NULL,
                    tier INTEGER NOT NULL,
                    features INTEGER NOT NULL,
                    average_load INTEGER DEFAULT 0,
                    physical_servers_json TEXT NOT NULL
                )"""
            )

            statement.executeUpdate(
                """CREATE TABLE IF NOT EXISTS servers_cache (
                    id INTEGER PRIMARY KEY,
                    cached_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    last_modified TEXT
                )"""
            )

            statement.executeUpdate(
                """CREATE TABLE IF NOT EXISTS recent_connections (
                    server_id TEXT PRIMARY KEY,
                    server_name TEXT NOT NULL,
                    city TEXT NOT NULL,
                    country TEXT NOT NULL,
                    last_connected_at INTEGER NOT NULL
                )"""
            )

            println("Database schema initialized")
        }
    }

    suspend fun getSession(): DesktopSessionEntity? = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext null
        try {
            connection.prepareStatement("SELECT * FROM session WHERE id = 1").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        DesktopSessionEntity(
                            id = resultSet.getInt("id"),
                            accessToken = cryptoManager.decrypt(resultSet.getString("access_token")),
                            refreshToken = cryptoManager.decrypt(resultSet.getString("refresh_token")),
                            sessionId = resultSet.getString("session_id"),
                            userId = resultSet.getString("user_id"),
                            userTier = resultSet.getInt("user_tier"),
                            wgPrivateKey = resultSet.getString("wg_private_key"),
                            wgPublicKeyPem = resultSet.getString("wg_public_key_pem"),
                            wgCertificate = resultSet.getString("wg_certificate")
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            println("Error getting session: ${e.message}")
            null
        }
    }

    suspend fun saveSession(session: DesktopSessionEntity) = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.prepareStatement(
                """INSERT OR REPLACE INTO session 
                (id, access_token, refresh_token, session_id, user_id, user_tier, wg_private_key, wg_public_key_pem, wg_certificate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            ).use { statement ->
                statement.setInt(1, session.id)
                statement.setString(2, cryptoManager.encrypt(session.accessToken))
                statement.setString(3, cryptoManager.encrypt(session.refreshToken))
                statement.setString(4, session.sessionId)
                statement.setString(5, session.userId)
                statement.setInt(6, session.userTier)
                statement.setString(7, session.wgPrivateKey)
                statement.setString(8, session.wgPublicKeyPem)
                statement.setString(9, session.wgCertificate)
                statement.executeUpdate()
            }
        } catch (e: Exception) {
            println("Error saving session: ${e.message}")
        }
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.createStatement().executeUpdate("DELETE FROM session")
        } catch (e: Exception) {
            println("Error clearing session: ${e.message}")
        }
    }

    suspend fun updateCertificate(certificate: String) = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.prepareStatement("UPDATE session SET wg_certificate = ? WHERE id = 1").use { statement ->
                statement.setString(1, certificate)
                statement.executeUpdate()
            }
        } catch (e: Exception) {
            println("Error updating certificate: ${e.message}")
        }
    }

    suspend fun updateVpnKeys(privateKey: String, publicKeyPem: String, certificate: String) =
        withContext(Dispatchers.IO) {
            val connection = connection ?: return@withContext
            try {
                connection.prepareStatement(
                    "UPDATE session SET wg_private_key = ?, wg_public_key_pem = ?, wg_certificate = ? WHERE id = 1"
                ).use { statement ->
                    statement.setString(1, privateKey)
                    statement.setString(2, publicKeyPem)
                    statement.setString(3, certificate)
                    statement.executeUpdate()
                }
            } catch (e: Exception) {
                println("Error updating VPN keys: ${e.message}")
            }
        }

    suspend fun getAllServers(): List<DesktopServerEntity> = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext emptyList()
        try {
            val servers = mutableListOf<DesktopServerEntity>()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM servers").use { resultSet ->
                    while (resultSet.next()) {
                        servers.add(
                            DesktopServerEntity(
                                id = resultSet.getString("id"),
                                name = resultSet.getString("name"),
                                city = resultSet.getString("city"),
                                exitCountry = resultSet.getString("exit_country"),
                                tier = resultSet.getInt("tier"),
                                features = resultSet.getInt("features"),
                                averageLoad = resultSet.getInt("average_load"),
                                physicalServersJson = resultSet.getString("physical_servers_json")
                            )
                        )
                    }
                }
            }
            servers
        } catch (e: Exception) {
            println("Error getting all servers: ${e.message}")
            emptyList()
        }
    }

    suspend fun insertServers(servers: List<DesktopServerEntity>) = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.autoCommit = false
            connection.prepareStatement(
                """INSERT OR REPLACE INTO servers 
                (id, name, city, exit_country, tier, features, average_load, physical_servers_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)"""
            ).use { statement ->
                for (server in servers) {
                    statement.setString(1, server.id)
                    statement.setString(2, server.name)
                    statement.setString(3, server.city)
                    statement.setString(4, server.exitCountry)
                    statement.setInt(5, server.tier)
                    statement.setInt(6, server.features)
                    statement.setInt(7, server.averageLoad)
                    statement.setString(8, server.physicalServersJson)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
            connection.commit()
            connection.autoCommit = true
        } catch (e: Exception) {
            connection.rollback()
            connection.autoCommit = true
            println("Error inserting servers: ${e.message}")
        }
    }

    suspend fun clearAllServers() = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.createStatement().executeUpdate("DELETE FROM servers")
        } catch (e: Exception) {
            println("Error clearing servers: ${e.message}")
        }
    }

    suspend fun getCacheInfo(): DesktopServersCacheEntity? = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext null
        try {
            connection.prepareStatement("SELECT * FROM servers_cache WHERE id = 1").use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        DesktopServersCacheEntity(
                            id = resultSet.getInt("id"),
                            cachedAt = resultSet.getLong("cached_at"),
                            expiresAt = resultSet.getLong("expires_at"),
                            lastModified = resultSet.getString("last_modified")
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            println("Error getting cache info: ${e.message}")
            null
        }
    }

    suspend fun saveCacheInfo(cache: DesktopServersCacheEntity) = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.prepareStatement(
                """INSERT OR REPLACE INTO servers_cache 
                (id, cached_at, expires_at, last_modified)
                VALUES (?, ?, ?, ?)"""
            ).use { statement ->
                statement.setInt(1, cache.id)
                statement.setLong(2, cache.cachedAt)
                statement.setLong(3, cache.expiresAt)
                statement.setString(4, cache.lastModified)
                statement.executeUpdate()
            }
        } catch (e: Exception) {
            println("Error saving cache info: ${e.message}")
        }
    }

    suspend fun clearCacheInfo() = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.createStatement().executeUpdate("DELETE FROM servers_cache")
        } catch (e: Exception) {
            println("Error clearing cache info: ${e.message}")
        }
    }

    // ===== Recent Connections =====

    suspend fun getRecentConnections(): List<DesktopRecentConnectionEntity> = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext emptyList()
        try {
            val list = mutableListOf<DesktopRecentConnectionEntity>()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM recent_connections ORDER BY last_connected_at DESC LIMIT 10").use { rs ->
                    while (rs.next()) {
                        list.add(
                            DesktopRecentConnectionEntity(
                                serverId = rs.getString("server_id"),
                                serverName = rs.getString("server_name"),
                                city = rs.getString("city"),
                                country = rs.getString("country"),
                                lastConnectedAt = rs.getLong("last_connected_at")
                            )
                        )
                    }
                }
            }
            list
        } catch (e: Exception) {
            println("Error getting recent connections: ${e.message}")
            emptyList()
        }
    }

    suspend fun addRecentConnection(entity: DesktopRecentConnectionEntity) = withContext(Dispatchers.IO) {
        val connection = connection ?: return@withContext
        try {
            connection.prepareStatement(
                "INSERT OR REPLACE INTO recent_connections (server_id, server_name, city, country, last_connected_at) VALUES (?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setString(1, entity.serverId)
                statement.setString(2, entity.serverName)
                statement.setString(3, entity.city)
                statement.setString(4, entity.country)
                statement.setLong(5, entity.lastConnectedAt)
                statement.executeUpdate()
            }
        } catch (e: Exception) {
            println("Error adding recent connection: ${e.message}")
        }
    }
}
