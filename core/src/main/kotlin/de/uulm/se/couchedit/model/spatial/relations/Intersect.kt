package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Relation specifying that the some of the area of [a] is included in [b] and vice versa.
 */
open class Intersect(a: ElementReference<ShapedElement<*>>, b: ElementReference<ShapedElement<*>>) : SpatialRelation(a, b) {
    override val isDirected: Boolean get() = false

    override fun copy() = Intersect(a, b)
}
