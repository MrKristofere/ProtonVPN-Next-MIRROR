/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.vpn

interface AmneziaConfigGenerator {
    fun buildConfig(
        serverPublicKey: String,
        privateKey: String,
        localIp: String,
        dnsServer: String,
        targetIp: String,
        isIncludeMode: Boolean = false,
        selectedApps: Set<String> = emptySet(),
        selectedIps: Set<String> = emptySet(),
        port: Int = 1194,
        certificate: String? = null,
        obfuscationParams: ObfuscationParams
    ): String
}
