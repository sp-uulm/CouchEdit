package de.uulm.se.couchedit.model.graphic.shapes

/**
 * Represents an ordinary rectangle specified by its top-left edge and its dimensions along the x and y axis.
 */
class Rectangle constructor(
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double = 0.0,
        h: Double = 0.0
) : Rectangular(x, y, w, h) {
    override fun toString(): String = "Rectangle(x=$x,y=$y,w=$w,h=$h)"

    override fun copy(): Rectangle = Rectangle(x, y, w, h)
}
