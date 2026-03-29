package ru.protonmod.next.desktop

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import ru.protonmod.next.data.network.*
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.desktop.native.VpnNative
import ru.protonmod.next.desktop.vpn.AmneziaUapiGenerator
import ru.protonmod.next.vpn.ObfuscationParams
import ru.protonmod.next.vpn.VpnConstants
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Base64

private const val BASE_URL = "https://vpn-api.proton.me/"

interface DesktopVpnApi {
    @GET("vpn/v2/logicals")
    suspend fun getLogicalServers(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Query("WithEntriesForProtocols") protocols: String = "wireguard",
        @Query("WithState") withState: Boolean = true,
        @Query("Tier") userTier: Int? = null
    ): Response<LogicalServersResponse>

    @GET("vpn/v2")
    suspend fun getVpnInfo(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String
    ): Response<ResponseBody>

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

class DesktopVpnClient(private val settingsManager: DesktopSettingsManager? = null) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val configGenerator = AmneziaUapiGenerator()

    private var privateKey: String? = null
    private var publicPem: String? = null
    private var helperProcess: Process? = null
    private var isSetupDone = false

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

            // Fetch loads separately
            val loadsResponse = vpnApi.getLoads(bearer, sessionId, userTier = userTier)
            val loadsMap = if (loadsResponse.isSuccessful) {
                loadsResponse.body()?.loads?.associate { it.id to it.load } ?: emptyMap()
            } else emptyMap()

            val servers = body.logicalServers.map { logical ->
                val logicalLoad = loadsMap[logical.id] ?: 0
                val physical = logical.servers.firstOrNull()?.copy()?.apply {
                    load = loadsMap[id] ?: logicalLoad
                }
                ServerEntry(
                    id = logical.id,
                    name = logical.name,
                    city = logical.city,
                    country = logical.entryCountry.ifBlank { logical.exitCountry },
                    tier = logical.tier,
                    physicalServer = physical
                )
            }
            Result.success(servers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ensureKeys() {
        if (privateKey == null || publicPem == null) {
            println("Generating new VPN keys...")
            val result = VpnNative.INSTANCE.GenerateWGKeys()
            if (result.startsWith("ERROR:")) throw Exception(result)
            
            // Format: X25519_PRIV_B64 ED25519_PUB_PEM_B64 X25519_PUB_B64
            val parts = result.split(" ")
            privateKey = parts[0]
            val pemB64 = parts[1]
            publicPem = String(Base64.getDecoder().decode(pemB64))
            
            println("Keys generated successfully.")
        }
    }

    suspend fun setupVpn(accessToken: String, sessionId: String): Result<Unit> {
        return try {
            println("Starting VPN setup...")
            val bearer = "Bearer $accessToken"
            
            // 1. Get VPN Info (vpn/v2)
            println("Fetching VPN info...")
            val vpnInfoResponse = vpnApi.getVpnInfo(bearer, sessionId)
            if (!vpnInfoResponse.isSuccessful) throw Exception("Failed to get VPN info: ${vpnInfoResponse.code()}")
            
            // 2. Ensure keys
            ensureKeys()
            
            // 3. Create certificate (register Ed25519 public key in PEM)
            println("Registering public key with Proton...")
            val certResponse = vpnApi.createCertificate(bearer, sessionId, CreateCertificateRequest(publicPem!!))
            if (!certResponse.isSuccessful) throw Exception("Failed to create certificate: ${certResponse.code()}")
            
            isSetupDone = true
            println("VPN setup completed successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            println("VPN setup failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun connect(accessToken: String, sessionId: String, server: ServerEntry): Result<Unit> {
        return try {
            if (!isSetupDone) {
                val setupResult = setupVpn(accessToken, sessionId)
                if (setupResult.isFailure) return setupResult
            }

            val physicalServer = server.physicalServer ?: throw Exception("No physical server for this logical server")
            val serverPublicKey = physicalServer.wgPublicKey ?: throw Exception("Server has no WG public key")
            val targetIp = physicalServer.exitIp ?: throw Exception("Server has no exit IP")

            // 4. Generate config
            val obfuscationParams = settingsManager?.getObfuscationParams() ?: ObfuscationParams(
                jc = 3, jmin = 1, jmax = 3, s1 = 0, s2 = 0,
                h1 = "1", h2 = "2", h3 = "3", h4 = "4",
                i1 = VpnConstants.DEFAULT_I1
            )
            
            val port = settingsManager?.settings?.value?.vpnPort ?: 1194

            val uapiConfig = configGenerator.buildConfig(
                serverPublicKey = serverPublicKey,
                privateKey = privateKey!!,
                localIp = VpnConstants.PROTON_CLIENT_IP,
                dnsServer = VpnConstants.PROTON_DNS_IP,
                targetIp = targetIp,
                port = port,
                obfuscationParams = obfuscationParams
            )

            println("Generated UAPI Config:\n$uapiConfig")

            // 5. Connect via root helper daemon
            startHelper("wg0", uapiConfig, VpnConstants.PROTON_CLIENT_IP, targetIp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun startHelper(iface: String, config: String, localIp: String, serverIp: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resourceDir = System.getProperty("compose.application.resources.dir")
            val paths = mutableListOf(
                File("libs/vpn-helper"),
                File("desktop/libs/vpn-helper")
            )
            resourceDir?.let { paths.add(0, File(it, "vpn-helper")) }

            val helperFile = paths.find { it.exists() } ?: return@withContext Result.failure(Exception("VPN Helper not found in any of: $paths"))
            val helperPath = helperFile.absolutePath

            println("Starting VPN helper via pkexec from $helperPath...")
            val process = ProcessBuilder("pkexec", helperPath, iface, localIp, serverIp)
                .redirectErrorStream(true)
                .start()

            val writer = OutputStreamWriter(process.outputStream)
            writer.write(config)
            writer.write("\n\n")
            writer.flush()

            val reader = process.inputStream.bufferedReader()
            var isConnected = false

            // Wait for helper to signal success with timeout (increased to 60s for password prompt)
            val result = withTimeoutOrNull(60000) {
                while (true) {
                    val line = reader.readLine() ?: break
                    println("Helper: $line")
                    if (line == "CONNECTED") {
                        isConnected = true
                        break
                    }
                    if (line.startsWith("ERROR:")) {
                        return@withTimeoutOrNull Result.failure<Unit>(Exception(line))
                    }
                }
                null
            }

            if (result != null) {
                process.destroy()
                return@withContext result
            }

            if (isConnected) {
                helperProcess = process
                Result.success(Unit)
            } else {
                process.destroy()
                Result.failure(Exception("Helper connection timeout or premature exit"))
            }
        } catch (e: Exception) {
            println("Failed to start helper: ${e.message}")
            Result.failure(e)
        }
    }

    fun disconnect(): Result<Unit> {
        helperProcess?.let {
            println("Disconnecting VPN...")
            it.destroy()
            try {
                if (!it.waitFor(5, TimeUnit.SECONDS)) {
                    it.destroyForcibly()
                }
            } catch (e: Exception) {
                it.destroyForcibly()
            }
            helperProcess = null
            println("Disconnected.")
            return Result.success(Unit)
        }
        return Result.failure(Exception("Not connected"))
    }
}
