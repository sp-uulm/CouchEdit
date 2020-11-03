package de.uulm.se.couchedit.statecharts.model.couch.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateContainer
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement

class ParentOf<A : StateContainer, B : StateElement>(a: ElementReference<A>, b: ElementReference<B>) :
        OneToOneRelation<A, B>(a, b), StateChartAbstractSyntaxElement {
    override val isDirected = true

    override fun copy() = ParentOf(a, b)

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
}
