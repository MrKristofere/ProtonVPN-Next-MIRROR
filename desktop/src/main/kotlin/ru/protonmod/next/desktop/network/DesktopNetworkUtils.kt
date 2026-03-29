/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

object DesktopNetworkConstants {
    const val BASE_URL = "https://vpn-api.proton.me/"
    const val SPOOFED_APP_VERSION = "5.16.31.0"
    const val SPOOFED_OS = "Android 14"
    const val SPOOFED_DEVICE = "Google Pixel 7"
}

class DesktopHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder: Request.Builder = original.newBuilder()
            .header("User-Agent", "ProtonVPN/${DesktopNetworkConstants.SPOOFED_APP_VERSION} (${DesktopNetworkConstants.SPOOFED_OS}; ${DesktopNetworkConstants.SPOOFED_DEVICE})")
            .header("x-pm-appversion", "android-vpn@${DesktopNetworkConstants.SPOOFED_APP_VERSION}-dev+play")
            .header("x-pm-apiversion", "4")
            .header("Accept", "application/json, application/vnd.protonmail.v1+json")

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
