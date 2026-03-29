package ru.protonmod.next.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    // JNA path is now handled lazily in VpnNative
    Window(onCloseRequest = ::exitApplication, title = "ProtonVPN Next") {
        App()
    }
}
