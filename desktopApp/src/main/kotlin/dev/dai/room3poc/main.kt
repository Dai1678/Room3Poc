package dev.dai.room3poc

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Room3Poc",
    ) {
        App()
    }
}