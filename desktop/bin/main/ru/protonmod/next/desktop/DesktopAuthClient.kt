package ru.protonmod.next.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.*
import kotlin.math.abs

private const val BASE_URL = "https://vpn-api.proton.me/"
private const val SPOOFED_APP_VERSION = "5.16.31.0"

// Match Android client parameters to avoid "platform desktop is not valid" rejection.
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

    @POST("auth/v4/credentialless")
    suspend fun performLoginLess(
        @Header("Authorization") authorization: String,
        @Header("x-pm-uid") sessionId: String,
        @Body payload: JsonObject,
        @Header("x-pm-human-verification-token") captchaToken: String? = null,
        @Header("x-pm-human-verification-token-type") captchaTokenType: String? = null
    ): LoginResponse
}

/**
 * Lightweight Proton auth client for desktop.
 *
 * Implements the same guest login flow as the Android app but without any Go libraries.
 */
class DesktopAuthClient {
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

    /**
     * Perform a guest (anonymous) login.
     * If Proton requires a captcha, a [CaptchaRequiredException] is returned.
     */
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
                // Clear the cached payload so subsequent logins use a fresh challenge (but keep the session)
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

        val deviceName = "${locale.displayCountry} Desktop".takeIf { it.isNotBlank() } ?: "Linux Desktop"
        val deviceHash = abs("${System.getProperty("os.name")}-${System.getProperty("os.arch")}".hashCode().toLong())

        return buildJsonObject {
            put("Payload", buildJsonObject {
                put("vpn-android-v4-challenge-0", buildJsonObject {
                    put("type", JsonPrimitive("me.proton.core.challenge.data.frame.ChallengeFrame.Device"))
                    put("v", JsonPrimitive(SPOOFED_APP_VERSION))
                    put("appLang", JsonPrimitive(locale.language))
                    put("timezone", JsonPrimitive(timezone.id))
                    // Use Android-like device parameters to avoid backend platform restrictions.
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
                } catch (_: Exception) {
                    // ignore parse errors
                }
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
