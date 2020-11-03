package de.uulm.se.couchedit.model.graphic.shapes

/**
 * Represents a point without width or height in the two-dimensional coordinate system.
 */
open class Point(var x: Double, var y: Double): Shape {
    override fun copy() = Point(x, y)

    override fun toString() = "($x, $y)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    operator fun plus(other: Point): Point {
        return Point(this.x + other.x, this.y + other.y)
    }

    operator fun times(scalar: Double): Point {
        return Point(this.x * scalar, this.y * scalar)
    }

    fun swap(): Point {
        return Point(y, x)
    }

    fun negate(negX: Boolean, negY: Boolean): Point {
        return Point(
                x * if (negX) -1 else 1,
                y * if (negY) -1 else 1
        )
    }
}
