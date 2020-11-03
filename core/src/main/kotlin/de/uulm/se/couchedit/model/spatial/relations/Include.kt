package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Relation specifying that the whole area of [b] is included in [a].
 */
class Include(from: ElementReference<ShapedElement<*>>, to: ElementReference<ShapedElement<*>>) : SpatialRelation(from, to) {
    override val isDirected: Boolean get() = true

    override fun copy() = Include(a, b)
}
