package de.uulm.se.couchedit.client.util.geom

import kotlin.math.pow
import kotlin.math.sqrt

fun pointDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
}
