package de.uulm.se.couchedit.model.attribute.elements

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation

/**
 * Relation representing that the [a] AttributeBag contains attributes somehow belonging to the [b] Element.
 */
class AttributesFor(a: ElementReference<AttributeBag>, b: ElementReference<Element>) : OneToOneRelation<AttributeBag, Element>(a, b) {
    override fun copy(): AttributesFor {
        return AttributesFor(a, b)
    }

    override val isDirected = true

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
}
