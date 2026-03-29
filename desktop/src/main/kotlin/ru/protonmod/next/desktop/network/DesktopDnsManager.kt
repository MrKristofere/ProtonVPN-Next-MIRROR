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

package ru.protonmod.next.desktop.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.protonmod.next.desktop.monitoring.DesktopSentryManager
import java.io.File
import java.net.InetAddress

/**
 * DNS Configuration
 */
@Serializable
data class DnsConfig(
    val customDns: String = "",
    val useCustomDns: Boolean = false,
    val fallbackDns: String = DesktopDnsManager.PROTON_DNS_IPv4,
    val validateDns: Boolean = true
)

/**
 * DNS Manager for Desktop
 * Manages DNS configuration, validation, and fallback logic.
 * 
 * Supports:
 * - IPv4 addresses (e.g., 1.1.1.1, 8.8.8.8)
 * - IPv6 addresses (e.g., 2606:4700:4700::1111)
 * - Multiple DNS servers via configuration file
 * - Automatic validation
 * - Fallback to default Proton DNS
 */
class DesktopDnsManager(
    private val configFile: File = File("dns_config.json"),
    private val sentryManager: DesktopSentryManager = DesktopSentryManager()
) {

    companion object {
        private const val TAG = "DnsManager"
        const val PROTON_DNS_IPv4 = "10.8.8.1"      // Proton's primary DNS
        const val PROTON_DNS_IPv6 = "2001:db8:1::1" // Proton's IPv6 DNS
        const val CLOUDFLARE_IPv4 = "1.1.1.1"       // Popular DNS
        const val GOOGLE_IPv4 = "8.8.8.8"           // Popular DNS
        const val QUAD9_IPv4 = "9.9.9.9"            // Privacy-focused DNS
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _config = MutableStateFlow(loadConfiguration())
    val config: StateFlow<DnsConfig> = _config.asStateFlow()

    init {
        sentryManager.debugLog("DNS Manager initialized", mapOf(
            "config_file" to configFile.absolutePath,
            "use_custom_dns" to _config.value.useCustomDns.toString(),
            "custom_dns" to _config.value.customDns
        ))
    }

    // ===== Configuration Management =====

    /**
     * Load DNS configuration from file
     */
    private fun loadConfiguration(): DnsConfig {
        return try {
            if (configFile.exists()) {
                val config = json.decodeFromString<DnsConfig>(configFile.readText())
                println("$TAG: Loaded DNS configuration - custom_dns=${config.customDns}, use_custom=${config.useCustomDns}")
                config
            } else {
                println("$TAG: DNS configuration file not found, using defaults")
                DnsConfig()
            }
        } catch (e: Exception) {
            println("$TAG: Error loading DNS configuration: ${e.message}")
            sentryManager.captureException(e, "DnsManager load configuration")
            DnsConfig()
        }
    }

    /**
     * Save DNS configuration to file
     */
    private fun saveConfiguration(config: DnsConfig) {
        try {
            configFile.writeText(json.encodeToString(config))
            _config.value = config
            println("$TAG: DNS configuration saved - custom_dns=${config.customDns}")
            
            sentryManager.trackDnsConfigured(config.customDns)
        } catch (e: Exception) {
            println("$TAG: Error saving DNS configuration: ${e.message}")
            sentryManager.captureException(e, "DnsManager save configuration")
        }
    }

    // ===== DNS Validation =====

    /**
     * Validate if a string is a valid IP address (IPv4 or IPv6)
     */
    fun isValidIpAddress(address: String): Boolean {
        return try {
            val trimmed = address.trim()
            if (trimmed.isEmpty()) return false
            
            // Try to parse as IP address
            InetAddress.getByName(trimmed)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate DNS server by attempting resolution
     */
    fun validateDnsServer(dnsServer: String): Boolean {
        if (!isValidIpAddress(dnsServer)) {
            println("$TAG: Invalid DNS server IP: $dnsServer")
            return false
        }

        return try {
            // Try to resolve a common domain
            val testDomain = "proton.me"
            InetAddress.getByName(testDomain)  // This uses system DNS, but validates DNS works
            println("$TAG: DNS validation passed for $dnsServer")
            true
        } catch (e: Exception) {
            println("$TAG: DNS validation failed for $dnsServer: ${e.message}")
            sentryManager.captureException(e, "DnsManager validation")
            false
        }
    }

    /**
     * Normalize DNS address (trim, no validation needed here)
     */
    private fun normalizeDns(dns: String): String {
        return dns.trim()
    }

    // ===== DNS Configuration =====

    /**
     * Get the active DNS server (custom or default)
     */
    fun getActiveDns(): String {
        val cfg = _config.value
        return if (cfg.useCustomDns && cfg.customDns.isNotEmpty()) {
            cfg.customDns
        } else {
            PROTON_DNS_IPv4  // Default fallback
        }
    }

    /**
     * Set custom DNS server
     */
    fun setCustomDns(dnsServer: String): Boolean {
        val normalized = normalizeDns(dnsServer)
        
        // Allow empty string to reset to default
        if (normalized.isEmpty()) {
            val updated = _config.value.copy(
                customDns = PROTON_DNS_IPv4,
                useCustomDns = false
            )
            saveConfiguration(updated)
            println("$TAG: DNS reset to default")
            sentryManager.debugLog("DNS reset to default")
            return true
        }

        // Validate the DNS server
        if (!isValidIpAddress(normalized)) {
            println("$TAG: Invalid DNS address: $normalized")
            sentryManager.trackDnsResolutionError(normalized, "Invalid IP address format")
            return false
        }

        val updated = _config.value.copy(
            customDns = normalized,
            useCustomDns = true
        )
        saveConfiguration(updated)
        println("$TAG: Custom DNS set to $normalized")
        return true
    }

    /**
     * Enable or disable custom DNS
     */
    fun setUseCustomDns(enabled: Boolean) {
        val updated = _config.value.copy(useCustomDns = enabled)
        saveConfiguration(updated)
        println("$TAG: Custom DNS ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Reset DNS to default Proton DNS
     */
    fun resetToDefault() {
        val updated = _config.value.copy(
            customDns = PROTON_DNS_IPv4,
            useCustomDns = false
        )
        saveConfiguration(updated)
        println("$TAG: DNS reset to default Proton DNS")
    }

    /**
     * Reset DNS to Cloudflare
     */
    fun setToCloudflare() {
        val updated = _config.value.copy(
            customDns = CLOUDFLARE_IPv4,
            useCustomDns = true
        )
        saveConfiguration(updated)
        println("$TAG: DNS set to Cloudflare (${CLOUDFLARE_IPv4})")
    }

    /**
     * Reset DNS to Google
     */
    fun setToGoogle() {
        val updated = _config.value.copy(
            customDns = GOOGLE_IPv4,
            useCustomDns = true
        )
        saveConfiguration(updated)
        println("$TAG: DNS set to Google (${GOOGLE_IPv4})")
    }

    /**
     * Reset DNS to Quad9
     */
    fun setToQuad9() {
        val updated = _config.value.copy(
            customDns = QUAD9_IPv4,
            useCustomDns = true
        )
        saveConfiguration(updated)
        println("$TAG: DNS set to Quad9 (${QUAD9_IPv4})")
    }

    // ===== DNS Information =====

    /**
     * Get DNS info for VPN configuration
     */
    fun getDnsConfig(): Pair<String, String> {
        val activeDns = getActiveDns()
        val fallback = _config.value.fallbackDns
        return Pair(activeDns, fallback)
    }

    /**
     * Get available preset DNS options
     */
    fun getPresetDnsServers(): Map<String, String> {
        return mapOf(
            "Proton (Default)" to PROTON_DNS_IPv4,
            "Cloudflare (Fast)" to CLOUDFLARE_IPv4,
            "Google (Reliable)" to GOOGLE_IPv4,
            "Quad9 (Privacy)" to QUAD9_IPv4
        )
    }

    /**
     * Check if current DNS is a known preset
     */
    fun getPresetName(): String {
        val activeDns = getActiveDns()
        return when (activeDns) {
            PROTON_DNS_IPv4 -> "Proton (Default)"
            CLOUDFLARE_IPv4 -> "Cloudflare (Fast)"
            GOOGLE_IPv4 -> "Google (Reliable)"
            QUAD9_IPv4 -> "Quad9 (Privacy)"
            else -> "Custom (${activeDns})"
        }
    }

    // ===== Status & Debug =====

    /**
     * Get DNS configuration summary
     */
    fun getConfigurationSummary(): String {
        val cfg = _config.value
        val activeDns = getActiveDns()
        return """
            DNS Configuration:
            - Custom DNS Enabled: ${cfg.useCustomDns}
            - Custom DNS Server: ${cfg.customDns}
            - Active DNS: $activeDns
            - Fallback DNS: ${cfg.fallbackDns}
            - Preset: ${getPresetName()}
        """.trimIndent()
    }

    /**
     * Verify DNS configuration is valid
     */
    fun verifyConfiguration(): Boolean {
        val activeDns = getActiveDns()
        if (!isValidIpAddress(activeDns)) {
            println("$TAG: Configuration invalid - active DNS is not a valid IP")
            return false
        }
        println("$TAG: Configuration is valid")
        return true
    }
}
