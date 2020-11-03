package de.uulm.se.couchedit.model.graphic.shapes

/**
 * Generic line spanning at least two points from its beginning to its end.
 */
abstract class Line : Shape {
    /**
     * Definite first point of the line (where the user has started dragging it, however there may also be "directed"
     * lines which have more directional meaning attached to them; in that case [start] and [end] would have semantic
     * meaning.
     */
    abstract var start: Point

    /**
     * Definite end point of the line, opposing [start].
     */
    abstract var end: Point

    /**
     * For correct behavior, subclasses must override this method with one also checking their own semantically relevant
     * properties.
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * For correct behavior, hashCode implementations must contain all semantically relevant properties.
     */
    abstract override fun hashCode(): Int

    abstract override fun copy(): Line
}
