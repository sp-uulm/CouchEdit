package de.uulm.se.couchedit.model.graphic.shapes

/**
 * Representation for a [Rectangular] shape with rounded corners.
 *
 * The handling of corners is as follows:
 * * The shape is limited by its [x], [y], [w], [h] coordinates.
 * * The corners are described by quadratic bezier curves.
 * * Each corner rounding begins at most [cornerRadius] from the corners of the outer rectangle.
 * * If [w] < 2 * [cornerRadius] or [h] < 2 * [cornerRadius] , then the corner width / height is reduced to
 *   [w] / 2 respectively [h] / 2.
 */
class RoundedRectangle(x: Double, y: Double, w: Double, h: Double, var cornerRadius: Double) : Rectangular(x, y, w, h) {
    override fun copy(): RoundedRectangle {
        return RoundedRectangle(x, y, w, h, cornerRadius)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && this.cornerRadius == (other as? RoundedRectangle)?.cornerRadius
    }

    override fun hashCode(): Int {
        return super.hashCode() + 31 * cornerRadius.hashCode()
    }
}
