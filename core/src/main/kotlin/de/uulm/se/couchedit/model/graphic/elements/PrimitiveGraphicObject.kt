package de.uulm.se.couchedit.model.graphic.elements

import de.uulm.se.couchedit.model.graphic.shapes.Shape

/**
 * Class for all elementary [GraphicObject]s which may not contain other [GraphicObject]s.
 *
 * The [content] of this Element, which may be freely set to any instance of [T], is identical to its [shape] and
 * is both what is displayed on screen and what is used as the basis for calculation.
 */
class PrimitiveGraphicObject<T : Shape>(
        id: String,
        var content: T
) : GraphicObject<T>(id, content::class.java) {
    override val shape: T
        get() = content

    override fun setFrom(other: GraphicObject<in T>) {
        if (other !is PrimitiveGraphicObject<*> || other.shapeClass != this.shapeClass) {
            throw IllegalArgumentException("Can't set $this from $other")
        }

        this.content = other.content.copy() as T

        this.z = other.z
    }

    fun setContentFrom(shape: Shape) {
        if (shape::class.java != this.shapeClass) {
            throw IllegalArgumentException("Expected a ${this.shapeClass} shape, got a ${shape::class.simpleName}")
        }

        @Suppress("UNCHECKED_CAST")
        this.content = shape.copy() as T
    }

    override fun copy(): PrimitiveGraphicObject<T> {
        val retContent = content.copy()

        if (!shapeClass.isInstance(retContent)) {
            throw IllegalArgumentException("Contract Violation! Return value of $shapeClass.copy() is " +
                    "expected to be $shapeClass or one of its subtypes, got ${retContent::class.java}")
        }

        val ret = PrimitiveGraphicObject(id, retContent as T)
        ret.z = this.z
        return ret
    }

    /**
     * Two [PrimitiveGraphicObject]s are equivalent, iff:
     * * Their contents are equal
     * * The general [GraphicObject.equals] rules apply
     */
    override fun contentEquivalent(other: Any?): Boolean {
        if (this === other) return true

        if (!super.contentEquivalent(other)) return false

        val graphicObject = other as PrimitiveGraphicObject<*>

        if (this.content != graphicObject.content) return false

        return true
    }

    override fun toString(): String = "PGO($id, $content, z=${zToString()})"
}
