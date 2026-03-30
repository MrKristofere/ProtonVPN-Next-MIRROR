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

package ru.protonmod.next.desktop.ui.utils

import androidx.compose.runtime.*
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object DesktopStrings {
    private var currentLanguage = "en"
    private val strings = mutableMapOf<String, String>()

    fun loadLanguage(lang: String) {
        currentLanguage = lang
        strings.clear()
        
        // Load default English first
        loadXml("values/strings.xml")
        
        // Load overrides if not English
        if (lang != "en") {
            val path = when (lang) {
                "be" -> "values-be/strings.xml"
                "fa" -> "values-fa/strings.xml"
                "ru" -> "values-ru/strings.xml"
                "uk" -> "values-uk/strings.xml"
                "zh" -> "values-zh/strings.xml"
                else -> null
            }
            path?.let { loadXml(it) }
        }
    }

    private fun loadXml(resourcePath: String) {
        try {
            val inputStream: InputStream = object {}.javaClass.classLoader.getResourceAsStream(resourcePath) 
                ?: return
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val name = node.attributes.getNamedItem("name").nodeValue
                val value = node.textContent
                strings[name] = value
            }
        } catch (e: Exception) {
            println("Error loading strings from $resourcePath: ${e.message}")
        }
    }

    fun get(key: String, vararg args: Any): String {
        val template = strings[key] ?: key
        return try {
            template.format(*args)
        } catch (e: Exception) {
            template
        }
    }

    // Individual helper methods for common strings to keep code clean
    @Composable fun app_name() = get("app_name")
    @Composable fun welcome_title() = get("welcome_title")
    @Composable fun welcome_subtitle() = get("welcome_subtitle")
    @Composable fun btn_continue_guest() = get("btn_continue_guest")
    @Composable fun btn_create_account() = get("btn_create_account")
    @Composable fun btn_login() = get("btn_login")
    @Composable fun btn_ok() = get("btn_ok")
    @Composable fun btn_get_started() = get("btn_get_started")

    // Onboarding
    @Composable fun onboarding_ready() = get("onboarding_ready")
    @Composable fun onboarding_encryption_title() = get("onboarding_encryption_title")
    @Composable fun onboarding_encryption_desc() = get("onboarding_encryption_desc")
    @Composable fun onboarding_network_title() = get("onboarding_network_title")
    @Composable fun onboarding_network_desc() = get("onboarding_network_desc")
    @Composable fun onboarding_accelerator_title() = get("onboarding_accelerator_title")
    @Composable fun onboarding_accelerator_desc() = get("onboarding_accelerator_desc")
    @Composable fun onboarding_amnezia_title() = get("onboarding_amnezia_title")
    @Composable fun onboarding_amnezia_desc() = get("onboarding_amnezia_desc")

    @Composable fun login_title() = get("login_title")
    @Composable fun login_subtitle() = get("login_subtitle")
    @Composable fun hint_username() = get("hint_username")
    @Composable fun hint_password() = get("hint_password")
    @Composable fun forgot_password() = get("forgot_password")
    @Composable fun desc_toggle_password() = get("desc_toggle_password")
    @Composable fun status_connected() = get("status_connected")
    @Composable fun status_connecting() = get("status_connecting")
    @Composable fun status_not_connected() = get("status_not_connected")
    @Composable fun btn_disconnect() = get("btn_disconnect")
    @Composable fun btn_quick_connect() = get("btn_quick_connect")
    @Composable fun settings_title() = get("settings_title")
    @Composable fun countries_title() = get("countries_title")
    @Composable fun profiles_title() = get("profiles_title")
    @Composable fun profiles_empty_title() = get("profiles_empty_title")
    @Composable fun profiles_empty_desc() = get("profiles_empty_desc")
    @Composable fun desc_create_profile() = get("desc_create_profile")
    @Composable fun settings_language() = get("settings_language")
    @Composable fun obfuscation_title() = get("obfuscation_title")
    @Composable fun obfuscation_enable() = get("obfuscation_enable")
    @Composable fun obfuscation_enable_desc() = get("obfuscation_enable_desc")
    @Composable fun protocol_title() = get("protocol_title")
    @Composable fun title_select_protocol() = get("title_select_protocol")
    @Composable fun settings_split_tunneling() = get("settings_split_tunneling")
    @Composable fun settings_split_tunneling_desc() = get("settings_split_tunneling_desc")
    @Composable fun settings_custom_dns() = get("settings_custom_dns")
    @Composable fun settings_custom_dns_default() = get("settings_custom_dns_default")
    @Composable fun settings_custom_dns_title() = get("settings_custom_dns_title")
    @Composable fun settings_error_reporting() = get("settings_error_reporting")
    @Composable fun settings_error_reporting_desc() = get("settings_error_reporting_desc")
    @Composable fun settings_st_mode() = get("settings_st_mode")
    @Composable fun st_mode_exclude() = get("st_mode_exclude")
    @Composable fun st_mode_include() = get("st_mode_include")
    @Composable fun st_remove_ip_desc() = get("st_remove_ip_desc")
    @Composable fun desc_back() = get("desc_back")
    @Composable fun desc_close() = get("desc_close")
    @Composable fun settings_connection() = get("settings_connection")
    @Composable fun settings_auto_connect() = get("settings_auto_connect")
    @Composable fun settings_auto_connect_desc() = get("settings_auto_connect_desc")
    @Composable fun settings_api_bypass() = get("settings_api_bypass")
    @Composable fun settings_api_bypass_desc() = get("settings_api_bypass_desc")
    @Composable fun settings_port() = get("settings_port")
    @Composable fun settings_port_auto() = get("settings_port_auto")
    @Composable fun settings_customization() = get("settings_customization")
    @Composable fun settings_app_theme() = get("settings_app_theme")
    @Composable fun settings_load_display_mode() = get("settings_load_display_mode")
    @Composable fun settings_privacy() = get("settings_privacy")
    @Composable fun settings_kill_switch() = get("settings_kill_switch")
    @Composable fun kill_switch_desc() = get("kill_switch_desc")
    @Composable fun settings_on() = get("settings_on")
    @Composable fun settings_off() = get("settings_off")
    @Composable fun settings_notifications() = get("settings_notifications")
    @Composable fun settings_about() = get("settings_about")
    @Composable fun settings_version(v: String) = get("settings_version", v)
    @Composable fun settings_debug() = get("settings_debug")
    @Composable fun debug_title() = get("debug_title")

    // Setup Flow
    @Composable fun setup_choose_language() = get("setup_choose_language")
    @Composable fun setup_choose_language_subtitle() = get("setup_choose_language_subtitle")
    @Composable fun btn_continue() = get("btn_continue")
    @Composable fun setup_vpn_obfuscation() = get("setup_vpn_obfuscation")
    @Composable fun setup_vpn_obfuscation_subtitle() = get("setup_vpn_obfuscation_subtitle")
    @Composable fun setup_secure_access() = get("setup_secure_access")
    @Composable fun setup_secure_access_subtitle() = get("setup_secure_access_subtitle")
    @Composable fun btn_next() = get("btn_next")
    @Composable fun btn_reset() = get("btn_reset")
    @Composable fun btn_randomize() = get("btn_randomize")
    @Composable fun desc_obfuscation_settings() = get("desc_obfuscation_settings")
    @Composable fun obfuscation_protocol_desc() = get("obfuscation_protocol_desc")

    // Obfuscation Labels
    @Composable fun h1_label() = get("h1_label")
    @Composable fun h2_label() = get("h2_label")
    @Composable fun h3_label() = get("h3_label")
    @Composable fun h4_label() = get("h4_label")
    @Composable fun s1_label() = get("s1_label")
    @Composable fun s2_label() = get("s2_label")
    @Composable fun i1_label() = get("i1_label")
    @Composable fun jc_label() = get("jc_label")
    @Composable fun jmin_label() = get("jmin_label")
    @Composable fun jmax_label() = get("jmax_label")
    @Composable fun obfuscation_category_scrambling() = get("obfuscation_category_scrambling")
    @Composable fun obfuscation_category_scrambling_advanced() = get("obfuscation_category_scrambling_advanced")
    @Composable fun obfuscation_category_magic() = get("obfuscation_category_magic")
    @Composable fun obfuscation_category_junk() = get("obfuscation_category_junk")

    // Split Tunneling
    @Composable fun st_enable() = get("st_enable")
    @Composable fun st_enable_desc() = get("st_enable_desc")
    @Composable fun st_apps_header() = get("st_apps_header")
    @Composable fun st_ips_header() = get("st_ips_header")
    @Composable fun st_domains_header() = get("st_domains_header")
    @Composable fun st_add_app() = get("st_add_app")
    @Composable fun st_add_ip() = get("st_add_ip")
    @Composable fun st_add_domain() = get("st_add_domain")
    @Composable fun st_input_app_label() = get("st_input_app_label")
    @Composable fun st_input_ip_label() = get("st_input_ip_label")
    @Composable fun st_input_domain_label() = get("st_input_domain_label")
    
    // Custom DNS
    @Composable fun dns_use_custom() = get("dns_use_custom")
    @Composable fun dns_use_custom_desc() = get("dns_use_custom_desc")
    @Composable fun dns_presets_header() = get("dns_presets_header")
    @Composable fun dns_custom_header() = get("dns_custom_header")
    @Composable fun dns_manual_ip() = get("dns_manual_ip")
    @Composable fun dns_input_label() = get("dns_input_label")
    @Composable fun dns_preset_proton() = get("dns_preset_proton")
    @Composable fun dns_preset_cloudflare() = get("dns_preset_cloudflare")
    @Composable fun dns_preset_google() = get("dns_preset_google")
    @Composable fun dns_preset_quad9() = get("dns_preset_quad9")

    
    // Sentry
    @Composable fun sentry_crash_reports() = get("settings_crash_reports")
    @Composable fun sentry_crash_reports_desc() = get("settings_crash_reports_desc")
    @Composable fun sentry_analytics() = get("settings_analytics")
    @Composable fun sentry_analytics_desc() = get("settings_analytics_desc")
    @Composable fun sentry_breadcrumbs() = get("sentry_breadcrumbs")
    @Composable fun sentry_breadcrumbs_desc() = get("sentry_breadcrumbs_desc")
    
    // Quick Connect
    @Composable fun captcha_proxy_active() = get("captcha_proxy_active")
    @Composable fun captcha_msg_open_browser() = get("captcha_msg_open_browser")
    @Composable fun captcha_msg_copy_token() = get("captcha_msg_copy_token")
    @Composable fun captcha_hint_token() = get("captcha_hint_token")
    @Composable fun btn_reopen_captcha() = get("btn_reopen_captcha")
    @Composable fun btn_open_captcha() = get("btn_open_captcha")
    @Composable fun btn_submit_token() = get("btn_submit_token")
    @Composable fun captcha_title() = get("captcha_title")
    @Composable fun captcha_message() = get("captcha_message")
    @Composable fun btn_cancel() = get("btn_cancel")
    
    // Quick Connect
    @Composable fun qc_title() = get("qc_title")
    @Composable fun qc_fastest() = get("qc_fastest")
    @Composable fun qc_fastest_desc() = get("qc_fastest_desc")
    @Composable fun qc_recent() = get("qc_recent")
    @Composable fun qc_recent_desc() = get("qc_recent_desc")
    @Composable fun qc_header_recent() = get("qc_header_recent")
}
