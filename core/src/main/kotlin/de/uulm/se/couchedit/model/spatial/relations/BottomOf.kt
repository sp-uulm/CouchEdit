package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Specifies that [a] Element is located beneath the [b] Element, which does not mean that all points of [a] are below
 * [b] but rather that at least one point of [a] is below [b] and no point of [a] is above [b].
 */
open class BottomOf(a: ElementReference<ShapedElement<*>>, b: ElementReference<ShapedElement<*>>) : Disjoint(a, b) {
    override fun copy() = BottomOf(a, b)
}
