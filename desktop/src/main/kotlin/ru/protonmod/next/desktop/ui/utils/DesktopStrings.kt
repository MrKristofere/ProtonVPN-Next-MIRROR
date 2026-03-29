/*
 * Copyright (C) 2026 SMH01
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
    @Composable fun settings_language() = get("settings_language")
    @Composable fun obfuscation_title() = get("obfuscation_title")
    @Composable fun protocol_title() = get("protocol_title")
}
