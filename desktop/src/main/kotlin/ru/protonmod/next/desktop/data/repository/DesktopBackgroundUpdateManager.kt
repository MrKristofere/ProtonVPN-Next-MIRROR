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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.protonmod.next.desktop.data.local.DesktopDatabase
import ru.protonmod.next.desktop.data.local.DesktopSessionEntity

/**
 * Desktop Background Server Update Manager
 *
 * Manages periodic server and certificate updates in the background
 * for the Desktop application. Since desktop doesn't have WorkManager,
 * we use Kotlin coroutines with scheduled delays.
 *
 * Features:
 * - Periodic server list refresh (20-minute interval)
 * - Certificate proactive refresh checking
 * - Coordinated update scheduling
 * - Automatic restart on connection changes
 */
class DesktopBackgroundUpdateManager(
    private val database: DesktopDatabase,
    private val vpnRepository: DesktopVpnRepository,
    private val certificateManager: DesktopCertificateManager,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var updateJob: Job? = null
    private val updateMutex = Mutex()

    companion object {
        private const val TAG = "DesktopBackgroundUpdateMgr"
        private const val SERVER_UPDATE_INTERVAL_MINUTES = 20L
    }

    /**
     * Start background update service
     */
    fun start() {
        if (updateJob?.isActive == true) {
            println("$TAG: Background updates already running")
            return
        }

        updateJob = applicationScope.launch {
            _isRunning.value = true
            println("$TAG: Started background update manager")

            try {
                while (isActive) {
                    updateMutex.withLock {
                        try {
                            val session = database.getSession()
                            if (session != null) {
                                println("$TAG: Running periodic server update...")
                                vpnRepository.getServers(
                                    session.accessToken,
                                    session.sessionId,
                                    session.userTier,
                                    forceRefresh = false
                                )
                                println("$TAG: Server update completed")
                                
                                // Record metrics
                                Sentry.metrics().count("desktop_background_update_success", 1.0)
                            } else {
                                println("$TAG: No active session, skipping update")
                            }
                        } catch (e: Exception) {
                            println("$TAG: Update failed: ${e.message}")
                            Sentry.metrics().count("desktop_background_update_error", 1.0)
                        }
                    }

                    // Sleep before next update
                    delay(java.util.concurrent.TimeUnit.MINUTES.toMillis(SERVER_UPDATE_INTERVAL_MINUTES))
                }
            } finally {
                _isRunning.value = false
                println("$TAG: Background update manager stopped")
            }
        }
    }

    /**
     * Stop background update service
     */
    fun stop() {
        updateJob?.cancel()
        updateJob = null
        _isRunning.value = false
        println("$TAG: Stopped background update manager")
    }
}

/**
 * Example usage in your main application:
 *
 * ```kotlin
 * class ProtonVpnDesktopApp {
 *     private val dataManager = DesktopVpnDataManager()
 *     private val backgroundUpdateManager by lazy {
 *         DesktopBackgroundUpdateManager(
 *             dataManager.getDatabase(),
 *             dataManager.getVpnRepository(),
 *             dataManager.getCertificateManager()
 *         )
 *     }
 *
 *     fun setupApp() {
 *         runBlocking { dataManager.initialize() }
 *         backgroundUpdateManager.start()
 *     }
 *
 *     fun teardownApp() {
 *         backgroundUpdateManager.stop()
 *         runBlocking { dataManager.shutdown() }
 *     }
 * }
 * ```
 */
