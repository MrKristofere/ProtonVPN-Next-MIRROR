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

package ru.protonmod.next.desktop.vpn

import ru.protonmod.next.vpn.AmneziaConfigGenerator
import ru.protonmod.next.vpn.ObfuscationParams
import java.util.Base64

class AmneziaUapiGenerator : AmneziaConfigGenerator {

    private fun b64toHex(base64: String): String {
        val bytes = Base64.getDecoder().decode(base64)
        return bytes.joinToString("") { "%02x".format(it) }
    }

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
        val sb = StringBuilder()
        
        // Device section (implicitly started)
        sb.append("private_key=").append(b64toHex(privateKey)).append("\n")
        
        // AmneziaWG specific fields
        sb.append("jc=").append(obfuscationParams.jc).append("\n")
        sb.append("jmin=").append(obfuscationParams.jmin).append("\n")
        sb.append("jmax=").append(obfuscationParams.jmax).append("\n")
        sb.append("s1=").append(obfuscationParams.s1).append("\n")
        sb.append("s2=").append(obfuscationParams.s2).append("\n")
        sb.append("s3=").append(obfuscationParams.s3).append("\n")
        sb.append("s4=").append(obfuscationParams.s4).append("\n")
        
        if (obfuscationParams.h1.isNotEmpty()) sb.append("h1=").append(obfuscationParams.h1).append("\n")
        if (obfuscationParams.h2.isNotEmpty()) sb.append("h2=").append(obfuscationParams.h2).append("\n")
        if (obfuscationParams.h3.isNotEmpty()) sb.append("h3=").append(obfuscationParams.h3).append("\n")
        if (obfuscationParams.h4.isNotEmpty()) sb.append("h4=").append(obfuscationParams.h4).append("\n")
        
        if (obfuscationParams.i1.isNotEmpty()) sb.append("i1=").append(obfuscationParams.i1).append("\n")
        if (obfuscationParams.i2.isNotEmpty()) sb.append("i2=").append(obfuscationParams.i2).append("\n")
        if (obfuscationParams.i3.isNotEmpty()) sb.append("i3=").append(obfuscationParams.i3).append("\n")
        if (obfuscationParams.i4.isNotEmpty()) sb.append("i4=").append(obfuscationParams.i4).append("\n")
        if (obfuscationParams.i5.isNotEmpty()) sb.append("i5=").append(obfuscationParams.i5).append("\n")

        // Peer section starts with public_key
        sb.append("public_key=").append(b64toHex(serverPublicKey)).append("\n")
        sb.append("endpoint=").append(targetIp).append(":").append(port).append("\n")
        sb.append("persistent_keepalive_interval=60\n")
        
        // Allowed IPs
        val allowedIps = if (isIncludeMode) {
            if (selectedIps.isEmpty()) listOf("0.0.0.0/0") else selectedIps.toList()
        } else {
            // Exclude mode: calculate complement of selected IPs
            if (selectedIps.isEmpty()) listOf("0.0.0.0/0")
            else ru.protonmod.next.vpn.IpSubnetCalculator.complementOfExcluded(selectedIps)
        }

        allowedIps.forEach {
            sb.append("allowed_ip=").append(it).append("\n")
        }

        return sb.toString()
    }
}
