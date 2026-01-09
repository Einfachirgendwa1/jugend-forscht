package org.einfachirgendwa1.jugendForscht

import androidx.compose.runtime.*
import androidx.compose.ui.window.application

fun main() = application {
    val s = mutableListOf(
        Sensor(1, 1),
        Sensor(1, -1),
        Sensor(-1, 1),
        Sensor(-1, -1)
    )

    var sensors by remember { mutableStateOf<List<Sensor>>(s) }

    LaunchedEffect(Unit) { loadData(sensors) }
    Frontend(::exitApplication, sensors)
}
