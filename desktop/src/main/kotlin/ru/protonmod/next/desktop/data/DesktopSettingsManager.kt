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
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.ui.theme.AppTheme
import ru.protonmod.next.vpn.ObfuscationParams
import ru.protonmod.next.vpn.VpnConstants
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
    val serverLoadDisplayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL
)

class DesktopSettingsManager(private val settingsFile: File = File("settings.json")) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DesktopSettings> = _settings.asStateFlow()

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

    fun setAppTheme(theme: AppTheme) {
        saveSettings(_settings.value.copy(appTheme = theme))
    }

    fun setServerLoadDisplayMode(mode: ServerLoadDisplayMode) {
        saveSettings(_settings.value.copy(serverLoadDisplayMode = mode))
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
}
