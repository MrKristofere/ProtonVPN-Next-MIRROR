package ru.protonmod.next.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File

fun main() = application {
    // Set JNA library path to include our custom Go bridge
    // Check both current dir and desktop/libs for development flexibility
    val paths = listOf(File("libs"), File("desktop/libs"))
    val existingPath = paths.find { it.exists() && it.isDirectory }
    
    existingPath?.let {
        System.setProperty("jna.library.path", it.absolutePath)
        println("Native library path set to: ${it.absolutePath}")
    }

    Window(onCloseRequest = ::exitApplication, title = "ProtonVPN Next") {
        App()
    }
}
