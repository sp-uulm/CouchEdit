package de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition

class EndpointFor(
        a: ElementReference<TransitionEndPoint>,
        b: ElementReference<Transition>
) : OneToOneRelation<TransitionEndPoint, Transition>(a, b) {
    override val isDirected: Boolean = true

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    override fun copy() = EndpointFor(a, b)
}
