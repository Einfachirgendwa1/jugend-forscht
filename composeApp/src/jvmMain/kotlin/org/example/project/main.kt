package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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

    val focusRequester = remember { FocusRequester() }
    var editedSensor by remember { mutableStateOf<Sensor?>(null) }
    var editingInput by remember { mutableStateOf("") }
    var sensors by remember { mutableStateOf<List<Sensor>>(s) }
    var aF by remember { mutableStateOf(0.5) }
    var j by remember { mutableStateOf(50000.0) }
    var t by remember { mutableStateOf(10.0) }
    var aS by remember { mutableStateOf(0.1) }

    fun getRadius(pegel: Double): Double {
        return (0.0124583 * ((j * aF * aS * t) / (r.squared() * pegel)).sqrt()).also { println(it) }
    }

    MaterialTheme(colorScheme) {
        Window(onCloseRequest = ::exitApplication, title = "Jugend Forscht") {
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
//                            DoubleInput(dS, double { dS = it }, "Abstand der Sensoren", 10f..1000f) { "${it.toInt()}m" }
                            DoubleInput(aS, double { aS = it }, "Fläche Sensor", 0.1f..1f) { "${it.roundTo(1)}m²" }
                        }
                        TextField(
                            value = editingInput,
                            onValueChange = { editingInput = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                editingInput.toDoubleOrNull()?.let { editedSensor?.pegel = it }
                                editedSensor = null
                            }),
                            colors = TextFieldDefaults.textFieldColors(
                                textColor = colorScheme.onSurface,
                                backgroundColor = colorScheme.surface
                            ),
                            modifier = Modifier
                                .width(300.dp)
                                .padding(0.dp, 20.dp)
                                .focusRequester(focusRequester)
                                .alpha(if (editedSensor == null) 0f else 1f)
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colorScheme.background)
                            .pointerInput(null) {
                                awaitPointerEventScope {
                                    handleInput(sensors) {
                                        editedSensor = it
                                        editingInput = ""
                                        focusRequester.requestFocus()
                                    }
                                }
                            }
                    ) {
                        val points = sensors.map {
                            Circle(it.rx, it.ry, getRadius(it.pegel)).also { circle ->
                                it.relative(center)

                                drawCircle(color = colorScheme.primary, radius = 10f, center = it.pos)
                                drawCircle(
                                    color = colorScheme.secondary,
                                    radius = circle.r.toFloat(),
                                    center = it.pos,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }

                        var p = Point(points.map { it.cx }.average(), points.map { it.cy }.average())

                        var step = 1.0
                        var bestValue = maxDistToCircles(p, points)
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
                                val v = maxDistToCircles(np, points)
                                if (v < bestValue) {
                                    bestValue = v
                                    p = np
                                    improved = true
                                    break
                                }
                            }
                            if (!improved) step *= 0.95
                        }

                        val center = center + Offset(p.x.toFloat(), p.y.toFloat())
                        drawCircle(color = colorScheme.error, radius = 25f, center = center)
                    }
                }
            }
        }
    }
}

suspend fun AwaitPointerEventScope.handleInput(
    sensors: List<Sensor>,
    pressed: (Sensor?) -> Unit
) {
    while (true) {
        val change = awaitPointerEvent().changes[0]

        val sensor = sensors.minBy { dist(change.position, it.pos) }
        val hover = (if (dist(change.position, sensor.pos) <= 25) sensor else null)

        if (change.pressed) pressed(hover)
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
    Column(modifier = Modifier.padding(10.dp)) {
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

class Sensor(x: Int, y: Int) {
    val rx = x.toDouble() * 150f
    val ry = y.toDouble() * 150f
    var pegel = 0.05

    var pos: Offset = Offset(0f, 0f)

    fun relative(center: Offset) {
        pos = center + Offset(rx.toFloat(), ry.toFloat())
    }
}

fun Double.sqrt(): Double = pow(0.5)
fun Double.squared(): Double = pow(2)
fun Double.roundTo(n: Int): Double = round(this * 10.0.pow(n)) / 10.0.pow(n)

fun maxDistToCircles(p: Point, circles: List<Circle>): Double = circles.maxOf { hypot(p.x - it.cx, p.y - it.cy) - it.r }
fun dist(a: Offset, b: Offset): Double = ((a.x - b.x).toDouble().squared() + (a.y - b.y).toDouble().squared()).sqrt()
fun double(func: (Double) -> Unit): (Float) -> Unit = { func(it.toDouble()) }

data class Circle(val cx: Double, val cy: Double, val r: Double)
data class Point(val x: Double, val y: Double)