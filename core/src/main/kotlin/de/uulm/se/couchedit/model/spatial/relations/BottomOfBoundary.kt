package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Specifies that all points of [a] are below all points of [b].
 */
class BottomOfBoundary(a: ElementReference<ShapedElement<*>>, b: ElementReference<ShapedElement<*>>) : BottomOf(a, b) {
    override fun copy() = BottomOfBoundary(a, b)
}
