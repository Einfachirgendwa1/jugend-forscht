package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.round

fun main() = application {
    val s = mutableListOf(
        Sensor(1, 1),
        Sensor(1, -1),
        Sensor(-1, 1),
        Sensor(-1, -1)
    )

    val state = rememberWindowState(placement = WindowPlacement.Maximized)
    var sensors by remember { mutableStateOf<List<Sensor>>(s) }
    var aF by remember { mutableStateOf(0.5) }
    var j by remember { mutableStateOf(50000.0) }
    var t by remember { mutableStateOf(10.0) }
    var aS by remember { mutableStateOf(0.1) }
    var dS by remember { mutableStateOf(4000.0) }
    var zoom by remember { mutableStateOf(0.02) }

    fun getRadius(pegel: Double): Double {
        return (0.0124583 * ((j * aF * aS * t) / (r.squared() * pegel)).sqrt())
    }

    MaterialTheme(colorScheme) {
        Window(onCloseRequest = ::exitApplication, title = "Jugend Forscht", state = state) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .safeContentPadding()
                    .padding(16.dp)
                    .fillMaxSize(),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Column(modifier = Modifier.background(colorScheme.surface)) {
                            DoubleInput(t, double { t = it }, "Einwirkungszeit", 0f..100f) { "${it.roundTo(1)}s" }
                            DoubleInput(j, double { j = it }, "Intensität", 10000f..100000f) { "${it.toInt()}W/m²" }
                            DoubleInput(aF, double { aF = it }, "Fläche Feuer", 0.1f..100f) { "${it.roundTo(1)}m²" }
                            DoubleInput(dS, double { dS = it }, "Abstand der Sensoren", 100f..10000f) {
                                "${it.toInt()}m"
                            }
                            DoubleInput(aS, double { aS = it }, "Fläche Sensor", 0.1f..1f) { "${it.roundTo(1)}m²" }
                            DoubleInput(zoom, double { zoom = it }, "Zoom", minZoom..maxZoom) { "${it.roundTo(3)}px/m" }
                        }
                    }
                    val textMeasurer = rememberTextMeasurer()
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colorScheme.background)
                            .pointerInput(null) {
                                awaitPointerEventScope {
                                    handleInput {
                                        zoom = (zoom * it).coerceIn(minZoom.toDouble(), maxZoom.toDouble())
                                    }
                                }
                            }
                    ) {
                        val circles = sensors.map {
                            val position = it.getMathematicalPosition(dS)
                            val visualCenter = position.projectOnScreen(zoom, center)
                            val r = getRadius(it.pegel)
                            it.visualCenter = visualCenter
                            drawCircle(color = colorScheme.primary, radius = 10f, center = visualCenter)
                            drawCircle(
                                color = colorScheme.secondary.copy(alpha = 0.2f),
                                radius = r.toFloat() * zoom.toFloat(),
                                center = visualCenter,
                            )

                            val text = "${(it.pegel * 100).roundTo(2)}cm"
                            val textStyle = TextStyle(fontSize = 11.sp, color = colorScheme.onSurface)
                            val layout = textMeasurer.measure(text, textStyle)
                            val topLeft = visualCenter - Offset(layout.size.width / 2f, layout.size.height / 2f + 20)

                            if (topLeft.x.absoluteValue <= size.width.absoluteValue && topLeft.y.absoluteValue <= size.height.absoluteValue) {
                                drawText(
                                    textMeasurer,
                                    text,
                                    topLeft = topLeft,
                                    style = textStyle
                                )
                            }

                            Circle(position, getRadius(it.pegel))
                        }

                        var p = Point(circles.map { it.pos.x }.average(), circles.map { it.pos.y }.average())

                        var step = 1.0
                        var bestValue = maxDistToCircles(p, circles)
                        val directions = listOf(
                            Point(1.0, 0.0),
                            Point(-1.0, 0.0),
                            Point(0.0, 1.0),
                            Point(0.0, -1.0)
                        )

                        repeat(20000) {
                            var improved = false
                            for (d in directions) {
                                val np = Point(p.x + d.x * step, p.y + d.y * step)
                                val v = maxDistToCircles(np, circles)
                                if (v < bestValue) {
                                    bestValue = v
                                    p = np
                                    improved = true
                                    break
                                }
                            }
                            if (!improved) step *= 0.95
                        }

                        val center = p.projectOnScreen(zoom, center)
                        drawCircle(color = colorScheme.error, radius = 25f, center = center)
                    }
                    Column(modifier = Modifier.background(colorScheme.surface)) {
                        for (i in 0..3) {
                            DoubleInput(
                                sensors[i].pegel * 100,
                                double { sensors[i].pegel = it / 100 },
                                "Pegelstand Sensor ${i + 1}",
                                0f..3f
                            ) { "${(sensors[i].pegel * 100).roundTo(2)}cm" }
                        }
                    }
                }
            }
        }
    }
}

suspend fun AwaitPointerEventScope.handleInput(multiplyZoom: (Double) -> Unit) {
    while (true) {
        val mouse = awaitPointerEvent().changes[0]

        val scrollDelta = mouse.scrollDelta.y
        when {
            scrollDelta > 0f -> multiplyZoom(1.1)
            scrollDelta < 0f -> multiplyZoom(0.9)
        }
    }
}

@Composable
fun DoubleInput(
    v: Double,
    cb: (Float) -> Unit,
    label: String,
    range: ClosedFloatingPointRange<Float>,
    display: (Double) -> String
) {
    Column(modifier = Modifier.scale(0.9f)) {
        Text(label, color = colorScheme.onBackground)
        Text(display(v), color = colorScheme.onBackground)
        Slider(
            value = v.toFloat(),
            onValueChange = cb,
            valueRange = range,
            modifier = Modifier.width(300.dp),
            colors = SliderDefaults.colors(
                thumbColor = colorScheme.onSurface,
                activeTrackColor = colorScheme.onSurface
            ),
        )
    }
}

const val r = 0.005
const val minZoom = 0.001f
const val maxZoom = 1f

class Sensor(val x: Int, val y: Int) {
    var pegel by mutableStateOf(0.01)

    var visualCenter: Offset = Offset.Zero

    fun getMathematicalPosition(dS: Double): Point = Point(x * dS, y * dS)
}

fun Double.sqrt(): Double = pow(0.5)
fun Double.squared(): Double = pow(2)
fun Double.roundTo(n: Int): Double = round(this * 10.0.pow(n)) / 10.0.pow(n)

fun maxDistToCircles(p: Point, circles: List<Circle>): Double {
    return circles.maxOf { hypot(p.x - it.pos.x, p.y - it.pos.y) - it.r }
}

fun double(func: (Double) -> Unit): (Float) -> Unit = { func(it.toDouble()) }

data class Circle(val pos: Point, val r: Double)
data class Point(val x: Double, val y: Double) {
    fun scale(zoom: Double): Offset = Offset(x.toFloat(), y.toFloat()) * zoom.toFloat()
    fun projectOnScreen(zoom: Double, center: Offset): Offset = center + scale(zoom)
}
