package ru.protonmod.next.desktop.native

import com.sun.jna.Library
import com.sun.jna.Native

interface VpnNative : Library {
    companion object {
        val INSTANCE: VpnNative = Native.load("govpn", VpnNative::class.java)
    }

    fun SRPCompute(username: String, password: String, b64salt: String, signedModulus: String, serverEphemeral: String, version: Int): String
    fun GenerateWGKeys(): String
    fun ConnectVpn(interfaceName: String, config: String): String
    fun DisconnectVpn(): String
    fun GetVpnStats(): String
}
