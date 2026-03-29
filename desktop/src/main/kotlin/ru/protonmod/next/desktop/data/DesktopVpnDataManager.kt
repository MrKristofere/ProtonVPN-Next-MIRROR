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

package ru.protonmod.next.desktop.data

import kotlinx.coroutines.*
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.data.repository.DesktopVpnRepository
import ru.protonmod.next.desktop.data.repository.DesktopCertificateManager
import ru.protonmod.next.desktop.monitoring.DesktopSentryManager
import ru.protonmod.next.desktop.network.DesktopSplitTunnelingManager
import ru.protonmod.next.desktop.network.DesktopDnsManager
import java.io.File

/**
 * Desktop VPN Data Initialization
 *
 * This class initializes all data layer components for desktop VPN functionality:
 * - SQLite database for persistence
 * - Server update repository with intelligent caching
 * - Certificate manager with proactive refresh
 * - Sentry monitoring and analytics
 * - Split tunneling and DNS management
 *
 * Usage in your main Application class:
 *
 * ```kotlin
 * class ProtonVpnApp {
 *     private val vpnDataManager by lazy { DesktopVpnDataManager() }
 *
 *     fun initializeApp() {
 *         vpnDataManager.initialize()
 *     }
 *
 *     fun onAppExit() {
 *         vpnDataManager.shutdown()
 *     }
 * }
 * ```
 */
class DesktopVpnDataManager {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Lazy initialization of components
    val settingsManager by lazy { DesktopSettingsManager() }
    
    val sentryManager by lazy { 
        val s = settingsManager.settings.value
        DesktopSentryManager(
            isMetricsEnabled = s.sentryMetricsEnabled,
            isCrashReportingEnabled = s.sentryCrashReportingEnabled,
            isAnalyticsEnabled = s.sentryAnalyticsEnabled
        ).also { sm ->
            // Update Sentry manager reactively when settings change
            applicationScope.launch {
                settingsManager.settings.collect { s ->
                    sm.isMetricsEnabled = s.sentryMetricsEnabled
                    sm.isCrashReportingEnabled = s.sentryCrashReportingEnabled
                    sm.isAnalyticsEnabled = s.sentryAnalyticsEnabled
                }
            }
        }
    }
    
    val splitTunnelingManager by lazy { DesktopSplitTunnelingManager(sentryManager = sentryManager) }
    val dnsManager by lazy { DesktopDnsManager(sentryManager = sentryManager) }

    val database by lazy { DesktopDatabase() }
    val vpnRepository by lazy { DesktopVpnRepository(database, applicationScope = applicationScope) }
    val certificateManager by lazy { DesktopCertificateManager(database, vpnRepository, applicationScope) }

    companion object {
        private const val TAG = "DesktopVpnDataManager"
    }

    /**
     * Initialize all data layer components
     * Call this during app startup in your main initialization routine
     */
    suspend fun initialize() {
        try {
            println("$TAG: Initializing Desktop VPN Data Layer...")

            // Connect to database
            database.connect()

            // Start background server updates
            vpnRepository.startAutoUpdate()

            // Start certificate refresh monitoring
            certificateManager.checkAndRefreshCertificateProactively()

            println("$TAG: Desktop VPN Data Layer initialized successfully")
        } catch (e: Exception) {
            println("$TAG: Initialization failed: ${e.message}")
            throw e
        }
    }

    /**
     * Shutdown all data layer components
     * Call this during app shutdown
     */
    suspend fun shutdown() {
        try {
            println("$TAG: Shutting down Desktop VPN Data Layer...")

            vpnRepository.stopAutoUpdate()
            certificateManager.stopCertificateRefresh()
            database.disconnect()

            println("$TAG: Desktop VPN Data Layer shutdown complete")
        } catch (e: Exception) {
            println("$TAG: Shutdown error: ${e.message}")
        }
    }
}
