/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.vpn

import org.amnezia.awg.config.Config
import org.amnezia.awg.config.Interface
import org.amnezia.awg.config.Peer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmneziaConfigGeneratorImpl @Inject constructor() : AmneziaConfigGenerator {
    override fun buildConfig(
        serverPublicKey: String,
        privateKey: String,
        localIp: String,
        dnsServer: String,
        targetIp: String,
        isIncludeMode: Boolean,
        selectedApps: Set<String>,
        selectedIps: Set<String>,
        port: Int,
        certificate: String?,
        obfuscationParams: ObfuscationParams
    ): String {
        val allowedIpsList = when {
            isIncludeMode -> if (selectedIps.isEmpty()) listOf("0.0.0.0/0") else selectedIps.toList()
            else -> if (selectedIps.isEmpty()) listOf("0.0.0.0/0") else IpSubnetCalculator.complementOfExcluded(selectedIps)
        }
        
        val peer = Peer.Builder()
            .parsePublicKey(serverPublicKey)
            .parseEndpoint("$targetIp:$port")
            .apply {
                allowedIpsList.forEach { parseAllowedIPs(it) }
            }
            .setPersistentKeepalive(60)
            .build()

        val ifaceBuilder = Interface.Builder()
            .parsePrivateKey(privateKey)
            .parseAddresses("$localIp/32")
            .parseDnsServers(dnsServer)
            .setMtu(1280)
            .setJunkPacketCount(obfuscationParams.jc)
            .setJunkPacketMinSize(obfuscationParams.jmin)
            .setJunkPacketMaxSize(obfuscationParams.jmax)
            .setInitPacketJunkSize(obfuscationParams.s1)
            .setResponsePacketJunkSize(obfuscationParams.s2)
            .setCookieReplyPacketJunkSize(obfuscationParams.s3)
            .setTransportPacketJunkSize(obfuscationParams.s4)
            .apply {
                if (obfuscationParams.h1.isNotEmpty()) setInitPacketMagicHeader(obfuscationParams.h1)
                if (obfuscationParams.h2.isNotEmpty()) setResponsePacketMagicHeader(obfuscationParams.h2)
                if (obfuscationParams.h3.isNotEmpty()) setUnderloadPacketMagicHeader(obfuscationParams.h3)
                if (obfuscationParams.h4.isNotEmpty()) setTransportPacketMagicHeader(obfuscationParams.h4)
            }

        if (obfuscationParams.i1.isNotEmpty()) ifaceBuilder.parseSpecialJunkI1(obfuscationParams.i1)
        if (obfuscationParams.i2.isNotEmpty()) ifaceBuilder.parseSpecialJunkI2(obfuscationParams.i2)
        if (obfuscationParams.i3.isNotEmpty()) ifaceBuilder.parseSpecialJunkI3(obfuscationParams.i3)
        if (obfuscationParams.i4.isNotEmpty()) ifaceBuilder.parseSpecialJunkI4(obfuscationParams.i4)
        if (obfuscationParams.i5.isNotEmpty()) ifaceBuilder.parseSpecialJunkI5(obfuscationParams.i5)

        if (selectedApps.isNotEmpty()) {
            if (isIncludeMode) {
                ifaceBuilder.parseIncludedApplications(selectedApps.joinToString(","))
            } else {
                ifaceBuilder.parseExcludedApplications(selectedApps.joinToString(","))
            }
        }

        val config = Config.Builder().setInterface(ifaceBuilder.build()).addPeer(peer).build()
        return config.toAwgQuickString(false, false)
    }
}
