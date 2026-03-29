package ru.protonmod.next.desktop

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.*
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import ru.protonmod.next.data.network.*
import ru.protonmod.next.desktop.native.VpnNative
import ru.protonmod.next.desktop.monitoring.DesktopSentryManager
import java.util.*
import kotlin.math.abs

private const val BASE_URL = "https://vpn-api.proton.me/"
private const val SPOOFED_APP_VERSION = "5.16.31.0"
private const val SPOOFED_OS = "Android 14"
private const val SPOOFED_DEVICE = "Google Pixel 7"
private const val SPOOFED_DEVICE_HASH = 53319294142L

interface DesktopAuthApi {
    @POST("auth/v4/sessions")
    suspend fun createAnonymousSession(
        @Body payload: JsonObject,
        @Header("x-pm-human-verification-token") captchaToken: String? = null,
        @Header("x-pm-human-verification-token-type") captchaTokenType: String? = null
    ): LoginResponse

    @POST("auth/v4/info")
    suspend fun getAuthInfo(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Body request: AuthInfoRequest,
        @Header("x-pm-human-verification-token") captchaToken: String? = null,
        @Header("x-pm-human-verification-token-type") captchaTokenType: String? = null
    ): AuthInfoResponse

    @POST("auth/v4")
    suspend fun performLogin(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Body request: LoginRequest,
        @Header("x-pm-human-verification-token") captchaToken: String? = null,
        @Header("x-pm-human-verification-token-type") captchaTokenType: String? = null
    ): LoginResponse

    @POST("auth/v4/credentialless")
    suspend fun performLoginLess(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Body payload: JsonObject,
        @Header("x-pm-human-verification-token") captchaToken: String? = null,
        @Header("x-pm-human-verification-token-type") captchaTokenType: String? = null
    ): LoginResponse
}

class DesktopAuthClient(private val sentryManager: DesktopSentryManager? = null) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val authApi: DesktopAuthApi by lazy {
        val mediaType = "application/json".toMediaType()
        val client = OkHttpClient.Builder()
            .addInterceptor(HeadersInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()
            .create(DesktopAuthApi::class.java)
    }

    private var pendingAnonToken: String? = null
    private var pendingAnonUid: String? = null
    private var pendingChallengePayload: JsonObject? = null

    suspend fun login(username: String, password: String, captchaToken: String? = null): Result<LoginResponse> {
        try {
            sentryManager?.addBreadcrumb("Initiating login", "auth")
            val tokenType = if (captchaToken != null) "captcha" else null
            
            // 1. Ensure anonymous session for initial requests
            if (pendingAnonToken == null || pendingAnonUid == null) {
                sentryManager?.addBreadcrumb("Creating anonymous session", "auth")
                val challengePayload = pendingChallengePayload ?: buildChallengePayload().also { pendingChallengePayload = it }
                val anonSession = authApi.createAnonymousSession(challengePayload, captchaToken, tokenType)
                pendingAnonToken = anonSession.accessToken
                pendingAnonUid = anonSession.sessionId
            }
            
            val anonToken = pendingAnonToken ?: throw Exception("Failed to obtain anonymous session token")
            val anonUid = pendingAnonUid ?: throw Exception("Failed to obtain anonymous session uid")
            val bearer = "Bearer $anonToken"
            
            // 2. Get Auth Info
            sentryManager?.addBreadcrumb("Fetching auth info", "auth")
            val info = authApi.getAuthInfo(bearer, anonUid, AuthInfoRequest(username), captchaToken, tokenType)
            if (info.code != 1000) {
                sentryManager?.addBreadcrumb("Auth info failed with code ${info.code}", "auth", level = io.sentry.SentryLevel.ERROR)
                return Result.failure(Exception("Auth info failed: ${info.code}"))
            }
            
            // 3. Compute SRP proofs using native bridge
            sentryManager?.addBreadcrumb("Computing SRP proofs", "auth")
            val proofsStr = VpnNative.INSTANCE.SRPCompute(
                username, password,
                info.salt ?: "",
                info.modulus ?: "",
                info.serverEphemeral ?: "",
                4 // Standard Proton SRP version
            )
            
            if (proofsStr.startsWith("ERROR:")) return Result.failure(Exception(proofsStr))
            val parts = proofsStr.split(" ")
            
            // 4. Perform real login
            sentryManager?.addBreadcrumb("Performing real login", "auth")
            val loginRequest = LoginRequest(
                username = username,
                clientEphemeral = parts[0],
                clientProof = parts[1],
                srpSession = info.srpSession ?: "",
                payload = buildChallengePayload()
            )
            
            val response = authApi.performLogin(bearer, anonUid, loginRequest, captchaToken, tokenType)
            if (response.code == 1000) {
                sentryManager?.addBreadcrumb("Login successful", "auth")
                return Result.success(response)
            }
            sentryManager?.addBreadcrumb("Login failed with code ${response.code}", "auth", level = io.sentry.SentryLevel.ERROR)
            return Result.failure(Exception("Login failed: Code ${response.code}"))
            
        } catch (e: Exception) {
            sentryManager?.addBreadcrumb("Login exception: ${e.message}", "auth", level = io.sentry.SentryLevel.ERROR)
            return handleHttpError(e)
        }
    }

    suspend fun loginAnonymous(captchaToken: String? = null): Result<LoginResponse> {
        try {
            val tokenType = if (captchaToken != null) "captcha" else null
            val challengePayload = pendingChallengePayload ?: buildChallengePayload().also { pendingChallengePayload = it }

            if (pendingAnonToken == null || pendingAnonUid == null) {
                val anonSession = authApi.createAnonymousSession(challengePayload, captchaToken, tokenType)
                pendingAnonToken = anonSession.accessToken
                pendingAnonUid = anonSession.sessionId
            }

            val anonToken = pendingAnonToken ?: throw Exception("Failed to obtain anonymous session token")
            val anonUid = pendingAnonUid ?: throw Exception("Failed to obtain anonymous session uid")
            val bearer = "Bearer $anonToken"

            val response = authApi.performLoginLess(bearer, anonUid, challengePayload, captchaToken, tokenType)

            if (response.code == 1000) {
                pendingChallengePayload = null
                return Result.success(response)
            }
            return Result.failure(Exception("Guest login failed: Code ${response.code}"))
        } catch (e: Exception) {
            return handleHttpError(e)
        }
    }

    private fun buildChallengePayload(): JsonObject {
        val locale = Locale.getDefault()
        val timezone = TimeZone.getDefault()
        return buildJsonObject {
            put("Payload", buildJsonObject {
                put("vpn-android-v4-challenge-0", buildJsonObject {
                    put("type", JsonPrimitive("me.proton.core.challenge.data.frame.ChallengeFrame.Device"))
                    put("v", JsonPrimitive(SPOOFED_APP_VERSION))
                    put("appLang", JsonPrimitive(locale.language))
                    put("timezone", JsonPrimitive(timezone.id))
                    put("deviceName", JsonPrimitive(SPOOFED_DEVICE_HASH))
                    put("regionCode", JsonPrimitive(locale.country.ifEmpty { "US" }))
                    put("timezoneOffset", JsonPrimitive(-(timezone.rawOffset / (1000 * 60))))
                    put("isJailbreak", JsonPrimitive(false))
                    put("preferredContentSize", JsonPrimitive("1.0"))
                    put("storageCapacity", JsonPrimitive(128.0))
                    put("isDarkmodeOn", JsonPrimitive(false))
                    put("keyboards", buildJsonArray { add(JsonPrimitive("com.google.android.inputmethod.latin")) })
                })
            })
        }
    }

    private fun handleHttpError(e: Exception): Result<LoginResponse> {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (e.code() == 422 && !errorBody.isNullOrBlank()) {
                try {
                    val parsed = json.decodeFromString(ProtonErrorResponse.serializer(), errorBody)
                    if (parsed.code == 9001) {
                        val url = parsed.details?.webUrl ?: ""
                        val token = parsed.details?.humanVerificationToken ?: ""
                        return Result.failure(CaptchaRequiredException(url, token, pendingAnonUid))
                    }
                    if (parsed.code == 12087) {
                        clearPendingAuth()
                        return Result.failure(Exception("Verification failed. Please try again."))
                    }
                } catch (_: Exception) {}
            }
            return Result.failure(Exception("HTTP ${e.code()}: ${errorBody ?: e.message()}"))
        }
        return Result.failure(e)
    }

    private fun clearPendingAuth() {
        pendingAnonToken = null
        pendingAnonUid = null
        pendingChallengePayload = null
    }
}

internal class HeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val requestBuilder: Request.Builder = original.newBuilder()
            .header("User-Agent", "ProtonVPN/$SPOOFED_APP_VERSION ($SPOOFED_OS; $SPOOFED_DEVICE)")
            .header("x-pm-appversion", "android-vpn@$SPOOFED_APP_VERSION-dev+play")
            .header("x-pm-apiversion", "4")
            .header("Accept", "application/vnd.protonmail.v1+json")

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
