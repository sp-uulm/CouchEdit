package de.uulm.se.couchedit.processing.common.testutils.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation

class SimpleTestOneToOneRelation(
        a: ElementReference<SimpleTestElement>,
        b: ElementReference<Element>,
        override val isDirected: Boolean
) : OneToOneRelation<SimpleTestElement, Element>(a, b) {
    override fun copy() = SimpleTestOneToOneRelation(a, b, isDirected)

    override var probability: ProbabilityInfo? = null
}
