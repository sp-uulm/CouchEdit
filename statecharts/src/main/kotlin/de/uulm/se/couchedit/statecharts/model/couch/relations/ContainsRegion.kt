package de.uulm.se.couchedit.statecharts.model.couch.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.statecharts.model.couch.elements.OrthogonalRegion
import de.uulm.se.couchedit.statecharts.model.couch.elements.State
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement

class ContainsRegion(a: ElementReference<State>, b: ElementReference<OrthogonalRegion>)
    : OneToOneRelation<State, OrthogonalRegion>(a, b), StateChartAbstractSyntaxElement {
    override fun copy() = ContainsRegion(a, b)

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    override val isDirected = true
}
