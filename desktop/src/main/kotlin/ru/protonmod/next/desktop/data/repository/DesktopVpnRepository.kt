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

package ru.protonmod.next.desktop.data.repository

import io.sentry.Sentry
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import ru.protonmod.next.data.network.*
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.data.local.DesktopServerEntity
import ru.protonmod.next.desktop.data.local.DesktopServersCacheEntity
import ru.protonmod.next.desktop.data.local.DesktopRecentConnectionEntity
import ru.protonmod.next.desktop.network.DesktopHeadersInterceptor
import ru.protonmod.next.desktop.network.DesktopNetworkConstants
import java.util.concurrent.TimeUnit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType

/**
 * Desktop API Interface for VPN operations
 */
interface DesktopVpnApiService {
    @GET("vpn/v2/logicals")
    suspend fun getLogicalServers(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
        @Query("WithEntriesForProtocols") protocols: String = "wireguard",
        @Query("WithState") withState: Boolean = true,
        @Query("Tier") userTier: Int? = null,
        @Header("x-pm-status-id") statusId: String? = null
    ): Response<LogicalServersResponse>

    @GET("vpn/v2")
    suspend fun getVpnInfo(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String
    ): Response<ResponseBody>

    @GET("vpn/v1/user/location")
    suspend fun getUserLocation(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String
    ): Response<ResponseBody>

    @GET("vpn/v1/servers/{id}/domain")
    suspend fun getServerDomain(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Path("id") serverId: String
    ): ConnectingDomainResponse

    @POST("vpn/v1/certificate")
    suspend fun createCertificate(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Body request: CreateCertificateRequest
    ): Response<CreateCertificateResponse>

    @GET("vpn/v1/loads")
    suspend fun getLoads(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Query("Tier") userTier: Int? = null
    ): Response<LoadsResponse>
}

/**
 * Desktop VPN Repository
 * Mirrors Android VpnRepository with server caching, certificate management, and auto-update logic
 */
class DesktopVpnRepository(
    private val database: DesktopDatabase,
    private val baseUrl: String = DesktopNetworkConstants.BASE_URL,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private var autoUpdateJob: Job? = null
    private val fetchMutex = Mutex()
    private var activeFetch: Deferred<Result<List<LogicalServer>>>? = null
    private var cachedServers: List<LogicalServer> = emptyList()

    companion object {
        private const val TAG = "DesktopVpnRepository"
        private const val CACHE_DURATION_MILLIS = 60 * 60 * 1000L // 1 hour
        private const val AUTO_UPDATE_INTERVAL_MINUTES = 20L
    }

    private val vpnApi: DesktopVpnApiService by lazy {
        val mediaType = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(DesktopHeadersInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()
            .create(DesktopVpnApiService::class.java)
    }

    /**
     * Start periodic server updates in the background
     */
    fun startAutoUpdate() {
        if (autoUpdateJob?.isActive == true) return

        autoUpdateJob = applicationScope.launch {
            println("$TAG: Starting periodic server load/list auto-update loop")
            while (isActive) {
                val session = database.getSession()
                if (session != null) {
                    println("$TAG: Auto-update: Fetching fresh server data for user tier ${session.userTier}")
                    getServers(
                        session.accessToken,
                        session.sessionId,
                        session.userTier,
                        forceRefresh = false
                    )
                } else {
                    println("$TAG: Auto-update: No active session, skipping this cycle")
                }
                delay(TimeUnit.MINUTES.toMillis(AUTO_UPDATE_INTERVAL_MINUTES))
            }
        }
    }

    /**
     * Stop periodic server updates
     */
    fun stopAutoUpdate() {
        autoUpdateJob?.cancel()
        autoUpdateJob = null
    }

    // ===== Recent Connections =====

    suspend fun getRecentConnections(): List<DesktopRecentConnectionEntity> {
        return database.getRecentConnections()
    }

    suspend fun addRecentConnection(entity: DesktopRecentConnectionEntity) {
        database.addRecentConnection(entity)
    }

    /**
     * Get servers with intelligent caching
     */
    suspend fun getServers(
        accessToken: String,
        sessionId: String,
        userTier: Int,
        forceRefresh: Boolean = false
    ): Result<List<LogicalServer>> {
        // If another fetch is in progress, wait for it or return its result
        activeFetch?.let { existing ->
            if (existing.isCompleted) {
                activeFetch = null
                return existing.await()
            } else {
                return existing.await()
            }
        }

        val deferred = applicationScope.async {
            fetchMutex.withLock {
                performGetServers(accessToken, sessionId, userTier, forceRefresh)
            }
        }

        activeFetch = deferred
        return deferred.await().also {
            activeFetch = null
        }
    }

    /**
     * Trigger background server refresh after a successful VPN connection
     * This delays for 10 seconds to allow VPN to establish and then refreshes servers.
     */
    fun refreshServersAfterConnection(accessToken: String, sessionId: String, userTier: Int) {
        applicationScope.launch {
            println("$TAG: Delayed server update scheduled in 10 seconds")
            delay(10000)
            getServers(accessToken, sessionId, userTier, forceRefresh = true)
        }
    }

    /**
     * Trigger background server refresh without waiting
     */
    fun refreshServersBackground(accessToken: String, sessionId: String, userTier: Int) {
        applicationScope.launch {
            getServers(accessToken, sessionId, userTier)
        }
    }

    /**
     * Core server fetching logic with caching and fallback
     */
    private suspend fun performGetServers(
        accessToken: String,
        sessionId: String,
        userTier: Int,
        forceRefresh: Boolean
    ): Result<List<LogicalServer>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val now = System.currentTimeMillis()
            val cacheInfo = database.getCacheInfo()

            val shouldCheckApi = forceRefresh || cacheInfo == null || now > cacheInfo.expiresAt
            val isStale = cacheInfo != null && (now - cacheInfo.cachedAt > TimeUnit.MINUTES.toMillis(AUTO_UPDATE_INTERVAL_MINUTES))

            println("$TAG: Server sync check: force=$forceRefresh, hasCache=${cacheInfo != null}, expired=${now > (cacheInfo?.expiresAt ?: 0)}, stale=$isStale")

            if (!shouldCheckApi && !isStale) {
                val dbServers = database.getAllServers().map { DesktopServerEntity.toDomain(it) }
                if (dbServers.isNotEmpty()) {
                    val result = dbServers.filter { it.tier <= userTier }
                    cachedServers = result
                    println("$TAG: Returning ${result.size} servers from local cache (API skip)")
                    return@withContext Result.success(result)
                }
            }

            val bearer = "Bearer $accessToken"
            val ifModifiedSince = if (!forceRefresh) cacheInfo?.lastModified else null

            println("$TAG: Fetching servers from Proton API... (If-Modified-Since: $ifModifiedSince, StatusID: ${cacheInfo?.statusId})")
            val response = vpnApi.getLogicalServers(
                authorization = bearer,
                sessionId = sessionId,
                ifModifiedSince = ifModifiedSince,
                protocols = "wireguard",
                userTier = if (userTier == 0) null else userTier,
                statusId = cacheInfo?.statusId
            )

            val (serversList, newLastModified, newStatusId) = when (response.code()) {
                304 -> {
                    println("$TAG: Proton API: Servers not modified (304). Re-using existing DB entries.")
                    val dbServers = database.getAllServers().map { DesktopServerEntity.toDomain(it) }
                    Triple(dbServers, cacheInfo?.lastModified, cacheInfo?.statusId)
                }
                200 -> {
                    val body = response.body()
                    if (body?.code == 1000) {
                        println("$TAG: Proton API: Received ${body.logicalServers.size} logical servers (StatusID: ${body.statusId})")
                        
                        val isSameStatus = body.statusId != null && body.statusId == cacheInfo?.statusId
                        if (isSameStatus && !forceRefresh) {
                            println("$TAG: StatusID matches. Skipping full server list processing.")
                            val dbServers = database.getAllServers().map { DesktopServerEntity.toDomain(it) }
                            Triple(dbServers, (response.headers()["Last-Modified"] ?: cacheInfo?.lastModified) ?: "", body.statusId)
                        } else {
                            Triple(body.logicalServers, response.headers()["Last-Modified"], body.statusId)
                        }
                    } else {
                        println("$TAG: Proton API Error: Code ${body?.code}")
                        return@withContext Result.failure(Exception("API error: ${body?.code}"))
                    }
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    println("$TAG: Proton API Error: getLogicalServers returned ${response.code()}. Body: $errorBody")

                    val dbServers = database.getAllServers().map { DesktopServerEntity.toDomain(it) }
                    if (dbServers.isNotEmpty()) {
                        println("$TAG: Falling back to DB servers due to API error")
                        return@withContext Result.success(dbServers.filter { it.tier <= userTier })
                    }
                    return@withContext Result.failure(Exception("Network error: ${response.code()}"))
                }
            }

            if (serversList.isEmpty()) {
                println("$TAG: Proton API: Server list is empty in response")
                return@withContext Result.failure(Exception("No servers available"))
            }

            // Fetch server loads and merge with current list
            println("$TAG: Fetching server loads for ${serversList.size} servers...")
            val loadsResponse = try {
                vpnApi.getLoads(bearer, sessionId, if (userTier == 0) null else userTier)
            } catch (e: Exception) {
                println("$TAG: Failed to initiate loads request: ${e.message}")
                null
            }

            if (loadsResponse?.isSuccessful == true) {
                val loadsData = loadsResponse.body()
                val loadsMap = loadsData?.loads?.associate { it.id to it.load } ?: emptyMap()
                println("$TAG: Successfully updated loads for ${loadsMap.size} server IDs")

                serversList.forEach { logical ->
                    val logicalLoad = loadsMap[logical.id]
                    if (logicalLoad != null) {
                        logical.averageLoad = logicalLoad
                        logical.servers.forEach { it.load = loadsMap[it.id] ?: logicalLoad }
                    } else {
                        var totalLoad = 0
                        var activeServers = 0
                        logical.servers.forEach { physical ->
                            val load = loadsMap[physical.id]
                            if (load != null) {
                                physical.load = load
                                totalLoad += load
                                activeServers++
                            }
                        }
                        if (activeServers > 0) logical.averageLoad = totalLoad / activeServers
                    }
                }
            } else {
                println("$TAG: Failed to fetch fresh server loads (HTTP ${loadsResponse?.code()}). Keeping existing loads.")
                val dbServers = database.getAllServers().associateBy({ it.id }, { it.averageLoad })
                serversList.forEach { it.averageLoad = dbServers[it.id] ?: 0 }
            }

            // Save to DB AFTER fetching loads
            val entities = serversList.map { DesktopServerEntity.fromDomain(it) }
            database.insertServers(entities)
            println("$TAG: Saved ${entities.size} servers to local database")

            // Update cache metadata
            val newCacheInfo = DesktopServersCacheEntity(
                cachedAt = now,
                expiresAt = now + CACHE_DURATION_MILLIS,
                lastModified = newLastModified,
                statusId = newStatusId
            )
            database.saveCacheInfo(newCacheInfo)

            val logicalServers = serversList.filter { it.tier <= userTier }
            cachedServers = logicalServers

            // Metrics
            val duration = System.currentTimeMillis() - startTime
            Sentry.metrics().distribution("server_fetch_latency", duration.toDouble())
            Sentry.metrics().count("server_fetch_success", 1.0)

            Result.success(logicalServers)
        } catch (e: Exception) {
            println("$TAG: Critical error in performGetServers: ${e.message}")

            // Metrics
            Sentry.metrics().count("server_fetch_error", 1.0)

            val dbServers = database.getAllServers().map { DesktopServerEntity.toDomain(it) }
            if (dbServers.isNotEmpty()) Result.success(dbServers.filter { it.tier <= userTier })
            else Result.failure(e)
        }
    }

    /**
     * Get user's current location and IP as seen by the API.
     */
    suspend fun getUserLocation(accessToken: String, sessionId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = vpnApi.getUserLocation("Bearer $accessToken", sessionId)
            val body = response.body()?.string()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to get location: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get server domain for connection
     */
    suspend fun getServerDomain(accessToken: String, sessionId: String, serverId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = vpnApi.getServerDomain("Bearer $accessToken", sessionId, serverId)
            val domain = response.domain
            if (response.code == 1000 && domain != null) {
                Result.success(domain)
            } else {
                Result.failure(Exception("Failed to get server domain: Code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get VPN user info
     */
    suspend fun getVpnInfo(accessToken: String, sessionId: String): Result<VpnInfoResponse> {
        return try {
            val response = vpnApi.getVpnInfo("Bearer $accessToken", sessionId)
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                try {
                    val vpnInfo = json.decodeFromString<VpnInfoResponse>(body)
                    Result.success(vpnInfo)
                } catch (e: Exception) {
                    println("$TAG: Failed to parse VPN info: ${e.message}")
                    Result.failure(e)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("$TAG: Failed to get VPN info: ${response.code()}. Body: $errorBody")
                Result.failure(Exception("Failed to get VPN info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register WireGuard key and get certificate
     */
    suspend fun registerWireGuardKey(
        accessToken: String,
        sessionId: String,
        publicKeyPem: String
    ): Result<CreateCertificateResponse> {
        return try {
            val bearer = "Bearer $accessToken"
            
            // Proton API sometimes requires calling vpn/v2 info before registration to initialize session state
            println("$TAG: Initializing VPN session info before registration...")
            getVpnInfo(accessToken, sessionId)

            val request = CreateCertificateRequest(publicKeyPem)
            val response = vpnApi.createCertificate(
                bearer,
                sessionId,
                request
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 1000) {
                    Result.success(body)
                } else {
                    println("$TAG: Certificate registration failed: Code ${body?.code}")
                    Result.failure(Exception("Certificate registration failed: Code ${body?.code}"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("$TAG: Certificate registration failed with code ${response.code()}. Body: $errorBody")
                Result.failure(Exception("Certificate registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
