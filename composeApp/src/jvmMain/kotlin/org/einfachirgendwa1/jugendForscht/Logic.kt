package org.einfachirgendwa1.jugendForscht

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.round

const val r = 0.005

class Sensor(val x: Int, val y: Int) {
    var pegel by mutableStateOf(0.01)

    var visualCenter: Offset = Offset.Zero

    fun circle(dS: Double, getRadius: (Double) -> Double): Circle {
        return Circle(Point(x * dS / 2, y * dS / 2), getRadius(pegel))
    }
}

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


fun positionOfFire(circles: List<Circle>): Point {
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

    return p
}