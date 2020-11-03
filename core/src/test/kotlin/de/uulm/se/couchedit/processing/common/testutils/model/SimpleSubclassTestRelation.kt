package de.uulm.se.couchedit.processing.common.testutils.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

class SimpleSubclassTestRelation(
        id: String,
        x: String,
        val y: String,
        isDirected: Boolean,
        aSet: Set<ElementReference<Element>>,
        bSet: Set<ElementReference<Element>>
) : SimpleTestRelation(id, x, isDirected, aSet, bSet) {
    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && (other as? SimpleSubclassTestRelation)?.y == this.y
    }

    override fun copy() = SimpleSubclassTestRelation(id, x, y, isDirected, aSet, bSet)
}
