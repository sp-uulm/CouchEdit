package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Specifies that all points of [a] are right of all points of [b].
 */
class RightOfBoundary(a: ElementReference<ShapedElement<*>>, b: ElementReference<ShapedElement<*>>) : RightOf(a, b) {
    override fun copy() = RightOfBoundary(a, b)
}
