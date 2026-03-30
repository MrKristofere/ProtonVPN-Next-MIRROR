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

package ru.protonmod.next.desktop.native

import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File

interface VpnNative : Library {
    companion object {
        val INSTANCE: VpnNative by lazy {
            val resourceDir = System.getProperty("compose.application.resources.dir")
            
            val paths = mutableListOf(File("libs"), File("desktop/libs"))
            resourceDir?.let { paths.add(0, File(it)) }
            
            println("Searching for native library in paths: ${paths.map { it.absolutePath }}")
            
            val existingPath = paths.find { File(it, "libgovpn.so").exists() }
            existingPath?.let {
                System.setProperty("jna.library.path", it.absolutePath)
                println("JNA path set to: ${it.absolutePath}")
            }
            
            try {
                Native.load("govpn", VpnNative::class.java)
            } catch (e: UnsatisfiedLinkError) {
                println("Failed to load govpn via JNA path, trying absolute path...")
                val libFile = paths.map { File(it, "libgovpn.so") }.find { it.exists() }
                if (libFile != null) {
                    println("Found libgovpn.so at: ${libFile.absolutePath}")
                    Native.load(libFile.absolutePath, VpnNative::class.java)
                } else {
                    throw e
                }
            }
        }
    }

    fun SRPCompute(username: String, password: String, b64salt: String, signedModulus: String, serverEphemeral: String, version: Int): String
    fun GenerateWGKeys(): String
    fun ConnectVpn(interfaceName: String, config: String): String
    fun DisconnectVpn(): String
    fun GetVpnStats(): String
}
