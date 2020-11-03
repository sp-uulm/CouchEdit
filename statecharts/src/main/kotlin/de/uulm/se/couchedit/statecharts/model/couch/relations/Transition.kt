package de.uulm.se.couchedit.statecharts.model.couch.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement

class Transition(
        override val id: String,
        a: ElementReference<StateElement>,
        b: ElementReference<StateElement>,
        var name: String?,
        override var probability: ProbabilityInfo?
) : OneToOneRelation<StateElement, StateElement>(a, b), StateChartAbstractSyntaxElement {
    override fun copy() = Transition(id, a, b, name, probability)

    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && this.name == (other as? Transition)?.name
    }

    override val isDirected = true
}
