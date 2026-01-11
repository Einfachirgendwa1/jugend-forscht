package org.einfachirgendwa1.jugendForscht

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.absoluteValue
import kotlin.math.pow

@Composable
fun Frontend(sensors: List<Sensor>, exitApplication: () -> Unit) {
    val state = rememberWindowState(placement = WindowPlacement.Maximized)
    var aF by remember { mutableStateOf(0.5) }
    var j by remember { mutableStateOf(50000.0) }
    var t by remember { mutableStateOf(10.0) }
    var aS by remember { mutableStateOf(0.1) }
    var dS by remember { mutableStateOf(4000.0) }
    var zoom by remember { mutableStateOf(0.02) }

    fun getRadius(pegel: Double): Double {
        return (0.0124583 * ((j * aF * aS * t) / (r.pow(2) * pegel)).pow(0.5))
    }

    MaterialTheme(colorScheme) {
        Window(onCloseRequest = exitApplication, title = "Jugend Forscht", state = state) {
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
                            },
                        onDraw = renderSensorsAndFire(sensors, dS, zoom, ::getRadius, textMeasurer)
                    )
                }
            }
        }
    }
}

fun renderSensorsAndFire(
    sensors: List<Sensor>,
    dS: Double,
    zoom: Double,
    getRadius: (Double) -> Double,
    textMeasurer: TextMeasurer
): DrawScope.() -> Unit = {
    val circles = sensors.map { sensor ->
        sensor.circle(dS, getRadius).also { circle ->
            sensor.render(circle, zoom, center, getRadius)()
            sensor.renderLabel(textMeasurer)()
        }
    }


    val firePos = positionOfFire(circles).projectOnScreen(zoom, center)
    drawCircle(color = colorScheme.error, radius = 20f, center = firePos)
}

fun Sensor.render(circle: Circle, zoom: Double, center: Offset, getRadius: (Double) -> Double): DrawScope.() -> Unit = {
    visualCenter = circle.pos.projectOnScreen(zoom, center)

    drawCircle(color = colorScheme.primary, radius = 10f, center = visualCenter)
    drawCircle(
        color = colorScheme.secondary.copy(alpha = 0.2f),
        radius = getRadius(pegel).toFloat() * zoom.toFloat(),
        center = visualCenter,
    )
}

fun Sensor.renderLabel(textMeasurer: TextMeasurer): DrawScope.() -> Unit = {
    val text = "${(pegel * 100).roundTo(2)}cm"
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

const val minZoom = 0.001f
const val maxZoom = 1f