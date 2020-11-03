package de.uulm.se.couchedit.processing.common.testutils.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.Relation

class OtherSimpleTestRelation(
        override val id: String,
        val x: String,
        override val isDirected: Boolean,
        aSet: Set<ElementReference<Element>>,
        bSet: Set<ElementReference<Element>>
) : Relation<Element, Element>(aSet, bSet) {
    override fun copy() = OtherSimpleTestRelation(id, x, isDirected, aSet, bSet)

    override var probability: ProbabilityInfo? = null

    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && (other as? OtherSimpleTestRelation)?.x == this.x
    }
}
