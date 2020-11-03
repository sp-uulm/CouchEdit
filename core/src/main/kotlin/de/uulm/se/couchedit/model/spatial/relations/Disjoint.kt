package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Superclass for all relations that specify that [a] and [b] do not have common (intersecting) points.
 */
abstract class Disjoint(
        a: ElementReference<ShapedElement<*>>,
        b: ElementReference<ShapedElement<*>>
) : SpatialRelation(a, b) {
    abstract override fun copy(): Disjoint

    override val isDirected: Boolean get() = true

    override fun toString(): String = "${this.javaClass.name}(a=$a,b=$b)"
}
