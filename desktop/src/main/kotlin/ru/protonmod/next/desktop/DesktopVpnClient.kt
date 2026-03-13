package ru.protonmod.next.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

private const val BASE_URL = "https://vpn-api.proton.me/"

@Serializable
data class LogicalServersResponse(
    @SerialName("Code") val code: Int,
    @SerialName("LogicalServers") val logicalServers: List<LogicalServer> = emptyList()
)

@Serializable
data class LogicalServer(
    @SerialName("ID") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Tier") val tier: Int,
    @SerialName("EntryCountry") val entryCountry: String,
    @SerialName("ExitCountry") val exitCountry: String,
    @SerialName("City") val city: String
)

interface DesktopVpnApi {
    @GET("vpn/v2/logicals")
    suspend fun getLogicalServers(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Query("WithEntriesForProtocols") protocols: String = "wireguard",
        @Query("WithState") withState: Boolean = true,
        @Query("Tier") userTier: Int? = null
    ): Response<LogicalServersResponse>
}

class DesktopVpnClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val vpnApi: DesktopVpnApi by lazy {
        val mediaType = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(HeadersInterceptor())
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()
            .create(DesktopVpnApi::class.java)
    }

    suspend fun getServers(accessToken: String, sessionId: String, userTier: Int? = null): Result<List<ServerEntry>> {
        return try {
            val bearer = "Bearer $accessToken"
            val response = vpnApi.getLogicalServers(bearer, sessionId, userTier = userTier)
            if (!response.isSuccessful) {
                return Result.failure(Exception("Server list request failed: ${response.code()}"))
            }
            val body = response.body()
            if (body == null || body.code != 1000) {
                return Result.failure(Exception("Server list response invalid: code=${body?.code}"))
            }
            val servers = body.logicalServers.map {
                ServerEntry(
                    id = it.id,
                    name = it.name,
                    city = it.city,
                    country = it.entryCountry.ifBlank { it.exitCountry },
                    tier = it.tier
                )
            }
            Result.success(servers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
