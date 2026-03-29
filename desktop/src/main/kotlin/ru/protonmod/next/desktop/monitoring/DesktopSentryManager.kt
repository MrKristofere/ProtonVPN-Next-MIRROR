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

package ru.protonmod.next.desktop.monitoring

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.Breadcrumb
import io.sentry.protocol.App
import io.sentry.protocol.Device
import java.time.Instant

/**
 * Desktop-specific Sentry monitoring manager.
 * Handles metrics, breadcrumbs, crash reporting, and analytics for the Desktop client.
 * 
 * Key Features:
 * - Server fetch metrics (latency, success/error counts)
 * - Certificate refresh tracking
 * - VPN connection monitoring
 * - Split tunneling and DNS configuration tracking
 * - Login/authentication metrics
 * - Controlled via user preferences
 */
class DesktopSentryManager(
    var isMetricsEnabled: Boolean = true,
    var isCrashReportingEnabled: Boolean = true,
    var isAnalyticsEnabled: Boolean = true,
    var isPerformanceTrackingEnabled: Boolean = true
) {

    companion object {
        const val TAG = "DesktopSentry"
    }

    // ===== Breadcrumb Tracking =====

    /**
     * Log a breadcrumb for debugging
     */
    fun addBreadcrumb(
        message: String,
        category: String = "general",
        level: SentryLevel = SentryLevel.INFO,
        data: Map<String, String> = emptyMap()
    ) {
        if (!isAnalyticsEnabled) return
        
        val breadcrumb = Breadcrumb().apply {
            this.message = message
            this.category = category
            this.level = level
            data.forEach { (key, value) ->
                this.setData(key, value)
            }
        }
        Sentry.addBreadcrumb(breadcrumb)
    }

    // ===== Metrics =====

    /**
     * Track server fetch latency
     */
    fun trackServerFetchLatency(durationMs: Long) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().distribution("server_fetch_latency", durationMs.toDouble())
            addBreadcrumb(
                "Server fetch latency recorded",
                "metrics",
                SentryLevel.DEBUG,
                mapOf("duration_ms" to durationMs.toString())
            )
        } catch (e: Exception) {
            println("Error tracking server fetch latency: ${e.message}")
        }
    }

    /**
     * Track successful server fetch
     */
    fun trackServerFetchSuccess(serverCount: Int = 0) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("server_fetch_success", 1.0)
            addBreadcrumb(
                "Server fetch successful",
                "server",
                SentryLevel.INFO,
                mapOf("server_count" to serverCount.toString())
            )
        } catch (e: Exception) {
            println("Error tracking server fetch success: ${e.message}")
        }
    }

    /**
     * Track server fetch error
     */
    fun trackServerFetchError(errorMessage: String = "Unknown") {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("server_fetch_error", 1.0)
            addBreadcrumb(
                "Server fetch failed",
                "server",
                SentryLevel.ERROR,
                mapOf("error" to errorMessage)
            )
        } catch (e: Exception) {
            println("Error tracking server fetch error: ${e.message}")
        }
    }

    /**
     * Track certificate refresh success
     */
    fun trackCertRefreshSuccess(expiresAt: Long = 0) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("cert_refresh_success", 1.0)
            val data = mutableMapOf<String, String>()
            if (expiresAt > 0) data["expires_at"] = expiresAt.toString()
            
            addBreadcrumb(
                "Certificate refreshed successfully",
                "certificate",
                SentryLevel.INFO,
                data
            )
        } catch (e: Exception) {
            println("Error tracking cert refresh success: ${e.message}")
        }
    }

    /**
     * Track certificate refresh failure
     */
    fun trackCertRefreshError(errorMessage: String = "Unknown") {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("cert_refresh_error", 1.0)
            addBreadcrumb(
                "Certificate refresh failed",
                "certificate",
                SentryLevel.ERROR,
                mapOf("error" to errorMessage)
            )
            
            if (isCrashReportingEnabled) {
                Sentry.captureMessage(
                    "Certificate refresh error: $errorMessage",
                    SentryLevel.ERROR
                )
            }
        } catch (e: Exception) {
            println("Error tracking cert refresh error: ${e.message}")
        }
    }

    /**
     * Track VPN connection attempt
     */
    fun trackVpnConnectionAttempt(
        server: String = "Unknown",
        protocol: String = "WireGuard",
        country: String = "Unknown"
    ) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("vpn_connection_attempt", 1.0)
            addBreadcrumb(
                "VPN connection attempt",
                "vpn",
                SentryLevel.INFO,
                mapOf(
                    "server" to server,
                    "protocol" to protocol,
                    "country" to country
                )
            )
        } catch (e: Exception) {
            println("Error tracking VPN connection attempt: ${e.message}")
        }
    }

    /**
     * Track successful VPN connection
     */
    fun trackVpnConnectionSuccess(server: String = "Unknown", durationMs: Long = 0) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("vpn_connection_success", 1.0)
            if (durationMs > 0) {
                Sentry.metrics().distribution("vpn_connection_latency", durationMs.toDouble())
            }
            addBreadcrumb(
                "VPN connection established",
                "vpn",
                SentryLevel.INFO,
                mapOf(
                    "server" to server,
                    "duration_ms" to durationMs.toString()
                )
            )
        } catch (e: Exception) {
            println("Error tracking VPN connection success: ${e.message}")
        }
    }

    /**
     * Track VPN connection failure
     */
    fun trackVpnConnectionError(server: String = "Unknown", errorMessage: String = "Unknown") {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("vpn_connection_error", 1.0)
            addBreadcrumb(
                "VPN connection failed",
                "vpn",
                SentryLevel.ERROR,
                mapOf(
                    "server" to server,
                    "error" to errorMessage
                )
            )
            
            if (isCrashReportingEnabled) {
                Sentry.captureMessage(
                    "VPN connection error on $server: $errorMessage",
                    SentryLevel.ERROR
                )
            }
        } catch (e: Exception) {
            println("Error tracking VPN connection error: ${e.message}")
        }
    }

    /**
     * Track VPN disconnection
     */
    fun trackVpnDisconnection(reason: String = "User") {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("vpn_disconnection", 1.0)
            addBreadcrumb(
                "VPN disconnected",
                "vpn",
                SentryLevel.INFO,
                mapOf("reason" to reason)
            )
        } catch (e: Exception) {
            println("Error tracking VPN disconnection: ${e.message}")
        }
    }

    // ===== Split Tunneling Metrics =====

    /**
     * Track split tunneling configuration change
     */
    fun trackSplitTunnelingConfigured(
        enabled: Boolean,
        mode: String,
        appCount: Int,
        ipCount: Int,
        domainCount: Int
    ) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("split_tunneling_configured", 1.0)
            addBreadcrumb(
                "Split tunneling configured",
                "split_tunneling",
                if (enabled) SentryLevel.INFO else SentryLevel.DEBUG,
                mapOf(
                    "enabled" to enabled.toString(),
                    "mode" to mode,
                    "apps" to appCount.toString(),
                    "ips" to ipCount.toString(),
                    "domains" to domainCount.toString()
                )
            )
        } catch (e: Exception) {
            println("Error tracking split tunneling config: ${e.message}")
        }
    }

    /**
     * Track app addition/removal from split tunneling
     */
    fun trackAppExclusionChanged(appName: String, added: Boolean) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("split_tunneling_app_changed", 1.0)
            addBreadcrumb(
                if (added) "App added to split tunneling" else "App removed from split tunneling",
                "split_tunneling",
                SentryLevel.DEBUG,
                mapOf("app" to appName, "action" to if (added) "added" else "removed")
            )
        } catch (e: Exception) {
            println("Error tracking app exclusion change: ${e.message}")
        }
    }

    // ===== DNS Metrics =====

    /**
     * Track custom DNS configuration change
     */
    fun trackDnsConfigured(dnsServer: String) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("dns_configured", 1.0)
            addBreadcrumb(
                "DNS server configured",
                "dns",
                SentryLevel.INFO,
                mapOf("dns_server" to dnsServer)
            )
        } catch (e: Exception) {
            println("Error tracking DNS configuration: ${e.message}")
        }
    }

    /**
     * Track DNS resolution error
     */
    fun trackDnsResolutionError(domain: String, errorMessage: String) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("dns_resolution_error", 1.0)
            addBreadcrumb(
                "DNS resolution failed",
                "dns",
                SentryLevel.WARNING,
                mapOf("domain" to domain, "error" to errorMessage)
            )
        } catch (e: Exception) {
            println("Error tracking DNS resolution error: ${e.message}")
        }
    }

    // ===== Authentication Metrics =====

    /**
     * Track login attempt
     */
    fun trackLoginAttempt() {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("login_attempt", 1.0)
            addBreadcrumb(
                "Login attempt",
                "authentication",
                SentryLevel.INFO
            )
        } catch (e: Exception) {
            println("Error tracking login attempt: ${e.message}")
        }
    }

    /**
     * Track login success
     */
    fun trackLoginSuccess(durationMs: Long) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("login_success", 1.0)
            Sentry.metrics().distribution("login_latency", durationMs.toDouble())
            addBreadcrumb(
                "Login successful",
                "authentication",
                SentryLevel.INFO,
                mapOf("duration_ms" to durationMs.toString())
            )
        } catch (e: Exception) {
            println("Error tracking login success: ${e.message}")
        }
    }

    /**
     * Track login failure
     */
    fun trackLoginError(errorMessage: String) {
        if (!isMetricsEnabled) return
        try {
            Sentry.metrics().count("login_error", 1.0)
            addBreadcrumb(
                "Login failed",
                "authentication",
                SentryLevel.ERROR,
                mapOf("error" to errorMessage)
            )
            
            if (isCrashReportingEnabled) {
                Sentry.captureMessage(
                    "Login error: $errorMessage",
                    SentryLevel.ERROR
                )
            }
        } catch (e: Exception) {
            println("Error tracking login error: ${e.message}")
        }
    }

    // ===== Exception Tracking =====

    /**
     * Capture and track an exception
     */
    fun captureException(throwable: Throwable, context: String = "Unknown") {
        if (!isCrashReportingEnabled) return
        try {
            addBreadcrumb(
                "Exception captured",
                "exception",
                SentryLevel.ERROR,
                mapOf("context" to context, "type" to throwable.javaClass.simpleName)
            )
            Sentry.captureException(throwable)
        } catch (e: Exception) {
            println("Error capturing exception: ${e.message}")
        }
    }

    /**
     * Capture a message
     */
    fun captureMessage(message: String, level: SentryLevel = SentryLevel.INFO) {
        if (!isAnalyticsEnabled) return
        try {
            Sentry.captureMessage(message, level)
        } catch (e: Exception) {
            println("Error capturing message: ${e.message}")
        }
    }

    // ===== User Context =====

    /**
     * Set user context for tracking
     */
    fun setUserContext(userId: String, email: String = "") {
        try {
            Sentry.setUser(
                io.sentry.protocol.User().apply {
                    this.id = userId
                    this.email = email
                }
            )
            addBreadcrumb(
                "User context set",
                "authentication",
                SentryLevel.DEBUG,
                mapOf("user_id" to userId)
            )
        } catch (e: Exception) {
            println("Error setting user context: ${e.message}")
        }
    }

    /**
     * Clear user context on logout
     */
    fun clearUserContext() {
        try {
            Sentry.setUser(null)
            addBreadcrumb(
                "User context cleared",
                "authentication",
                SentryLevel.DEBUG
            )
        } catch (e: Exception) {
            println("Error clearing user context: ${e.message}")
        }
    }

    // ===== Tag Management =====

    /**
     * Set a tag for grouping issues
     */
    fun setTag(key: String, value: String) {
        try {
            Sentry.setTag(key, value)
        } catch (e: Exception) {
            println("Error setting tag: ${e.message}")
        }
    }

    /**
     * Set multiple tags at once
     */
    fun setTags(tags: Map<String, String>) {
        try {
            tags.forEach { (key, value) ->
                Sentry.setTag(key, value)
            }
        } catch (e: Exception) {
            println("Error setting tags: ${e.message}")
        }
    }

    // ===== Debug =====

    /**
     * Log debug information
     */
    fun debugLog(message: String, data: Map<String, String> = emptyMap()) {
        if (!isAnalyticsEnabled) return
        println("DEBUG: $message")
        addBreadcrumb(message, "debug", SentryLevel.DEBUG, data)
    }
}
