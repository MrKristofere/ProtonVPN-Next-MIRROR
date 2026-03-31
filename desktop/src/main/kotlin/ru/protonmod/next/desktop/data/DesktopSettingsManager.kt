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

/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.protonmod.next.vpn.ObfuscationParams
import ru.protonmod.next.vpn.VpnConstants
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.ui.theme.AppTheme
import java.io.File

@Serializable
data class DesktopSettings(
    val killSwitchEnabled: Boolean = false,
    val autoConnectEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val vpnPort: Int = 1194,
    val obfuscationEnabled: Boolean = true,
    val jc: Int = 3,
    val jmin: Int = 1,
    val jmax: Int = 3,
    val s1: Int = 0,
    val s2: Int = 0,
    val h1: String = "1",
    val h2: String = "2",
    val h3: String = "3",
    val h4: String = "4",
    val i1: String = VpnConstants.DEFAULT_I1,
    val appTheme: AppTheme = AppTheme.DARK,
    val serverLoadDisplayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL,
    val language: String = "en",
    val isFirstRun: Boolean = true,
    val isLoggedIn: Boolean = false,
    
    @Deprecated("Use DesktopDatabase for secure session storage")
    val accessToken: String? = null,
    @Deprecated("Use DesktopDatabase for secure session storage")
    val sessionId: String? = null,

    // Sentry & Analytics
    val sentryMetricsEnabled: Boolean = true,
    val sentryCrashReportingEnabled: Boolean = true,
    val sentryAnalyticsEnabled: Boolean = true,
    // Split Tunneling
    val splitTunnelingEnabled: Boolean = false,
    val splitTunnelingMode: String = "exclude",
    // Custom DNS
    val customDns: String = "",
    val useCustomDns: Boolean = false,
    // Quick Connect
    val quickConnectStrategy: String = "fastest",
    val quickConnectTargetId: String? = null
)

class DesktopSettingsManager(private val settingsFile: File = File("settings.json")) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        explicitNulls = false
    }
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DesktopSettings> = _settings.asStateFlow()

    init {
        // Initialize strings with saved language
        ru.protonmod.next.desktop.ui.utils.DesktopStrings.loadLanguage(_settings.value.language)
    }

    private fun loadSettings(): DesktopSettings {
        return try {
            if (settingsFile.exists()) {
                json.decodeFromString<DesktopSettings>(settingsFile.readText())
            } else {
                DesktopSettings()
            }
        } catch (e: Exception) {
            println("Error loading settings: ${e.message}")
            DesktopSettings()
        }
    }

    private fun saveSettings(newSettings: DesktopSettings) {
        try {
            settingsFile.writeText(json.encodeToString(newSettings))
            _settings.value = newSettings
        } catch (e: Exception) {
            println("Error saving settings: ${e.message}")
        }
    }

    fun setKillSwitchEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(killSwitchEnabled = enabled))
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(autoConnectEnabled = enabled))
    }

    fun setVpnPort(port: Int) {
        saveSettings(_settings.value.copy(vpnPort = port))
    }

    fun setObfuscationEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(obfuscationEnabled = enabled))
    }

    fun setObfuscationParams(params: ObfuscationParams) {
        saveSettings(_settings.value.copy(
            jc = params.jc, jmin = params.jmin, jmax = params.jmax,
            s1 = params.s1, s2 = params.s2,
            h1 = params.h1, h2 = params.h2, h3 = params.h3, h4 = params.h4,
            i1 = params.i1
        ))
    }

    fun getObfuscationParams(): ObfuscationParams {
        val s = _settings.value
        return ObfuscationParams(
            jc = s.jc, jmin = s.jmin, jmax = s.jmax,
            s1 = s.s1, s2 = s.s2,
            h1 = s.h1, h2 = s.h2, h3 = s.h3, h4 = s.h4,
            i1 = s.i1
        )
    }

    fun setAppTheme(theme: AppTheme) {
        saveSettings(_settings.value.copy(appTheme = theme))
    }

    fun setServerLoadDisplayMode(mode: ServerLoadDisplayMode) {
        saveSettings(_settings.value.copy(serverLoadDisplayMode = mode))
    }

    fun setLanguage(lang: String) {
        saveSettings(_settings.value.copy(language = lang))
        ru.protonmod.next.desktop.ui.utils.DesktopStrings.loadLanguage(lang)
    }

    fun setFirstRunComplete() {
        saveSettings(_settings.value.copy(isFirstRun = false))
    }

    @Deprecated("Use DesktopDatabase.saveSession")
    fun saveSession(accessToken: String, sessionId: String) {
        // We only set the isLoggedIn flag in settings.json
        // The actual tokens are saved to the secure database by the ViewModel
        saveSettings(_settings.value.copy(isLoggedIn = true))
    }

    fun clearSession() {
        saveSettings(_settings.value.copy(
            isLoggedIn = false,
            accessToken = null, 
            sessionId = null
        ))
    }

    // ===== Sentry & Analytics =====

    fun setSentryMetricsEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(sentryMetricsEnabled = enabled))
    }

    fun setSentryCrashReportingEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(sentryCrashReportingEnabled = enabled))
    }

    fun setSentryAnalyticsEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(sentryAnalyticsEnabled = enabled))
    }

    // ===== Split Tunneling =====

    fun setSplitTunnelingEnabled(enabled: Boolean) {
        saveSettings(_settings.value.copy(splitTunnelingEnabled = enabled))
    }

    fun setSplitTunnelingMode(mode: String) {
        if (mode in listOf("exclude", "include")) {
            saveSettings(_settings.value.copy(splitTunnelingMode = mode))
        }
    }

    // ===== Custom DNS =====

    fun setCustomDns(dns: String) {
        saveSettings(_settings.value.copy(customDns = dns))
    }

    fun setUseCustomDns(enable: Boolean) {
        saveSettings(_settings.value.copy(useCustomDns = enable))
    }

    // ===== Quick Connect =====

    fun setQuickConnectStrategy(strategy: String, targetId: String? = null) {
        saveSettings(_settings.value.copy(
            quickConnectStrategy = strategy,
            quickConnectTargetId = targetId
        ))
    }

    fun getActiveDns(): String {
        val s = _settings.value
        return if (s.useCustomDns && s.customDns.isNotEmpty()) {
            s.customDns
        } else {
            "1.1.1.1"  // Default
        }
    }
}
