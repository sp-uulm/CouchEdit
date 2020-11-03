package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Specifies that [a] Element is located right of the [b] Element, which does not mean that all points of [a] are right of
 * [b] but rather that at least one point of [a] is right of [b] and no point of [a] is left of [b]..
 */
open class RightOf(a: ElementReference<ShapedElement<*>>, b: ElementReference<ShapedElement<*>>) : Disjoint(a, b) {
    override fun copy() = RightOf(a, b)
}
