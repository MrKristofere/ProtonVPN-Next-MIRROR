package ru.protonmod.next.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ProtonVPN Next") {
        App()
    }
}
