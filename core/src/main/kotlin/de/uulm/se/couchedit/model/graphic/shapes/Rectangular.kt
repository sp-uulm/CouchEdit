package de.uulm.se.couchedit.model.graphic.shapes

/**
 * Represents an ordinary rectangle specified by its top-left edge and its dimensions along the x and y axis.
 */
abstract class Rectangular constructor(
        var x: Double = 0.0,
        var y: Double = 0.0,
        var w: Double = 0.0,
        var h: Double = 0.0
) : Shape {
    abstract override fun copy(): Rectangular

    override fun toString(): String = "Rectangular Type(x=$x,y=$y,w=$w,h=$h])"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rectangular

        if (x != other.x) return false
        if (y != other.y) return false
        if (w != other.w) return false
        if (h != other.h) return false

        return true
    }

    override fun hashCode(): Int {
        var result = this::class.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + w.hashCode()
        result = 31 * result + h.hashCode()
        return result
    }
}
