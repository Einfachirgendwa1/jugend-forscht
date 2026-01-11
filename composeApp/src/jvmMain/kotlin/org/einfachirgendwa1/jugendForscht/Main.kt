package org.einfachirgendwa1.jugendForscht

import androidx.compose.runtime.*
import androidx.compose.ui.window.application

val onAppClose: MutableList<() -> Unit> = mutableListOf()

fun main() = application {
    val s = mutableListOf(
        Sensor(1, 1, 1),
        Sensor(1, -1, 2),
        Sensor(-1, 1, 3),
        Sensor(-1, -1, 4)
    )

    var sensors by remember { mutableStateOf<List<Sensor>>(s) }

    LaunchedEffect(Unit) { loadData(sensors) }
    Frontend(sensors) {
        onAppClose.forEach { it() }
        exitApplication()
    }
}
