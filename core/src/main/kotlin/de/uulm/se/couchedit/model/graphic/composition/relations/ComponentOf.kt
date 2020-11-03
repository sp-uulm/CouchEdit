package de.uulm.se.couchedit.model.graphic.composition.relations

import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

/**
 * Represents that [a] is a sub-element of [b], a and b being some kind of GraphicObjects.
 */
class ComponentOf<out A : GraphicObject<*>, out B : GraphicObject<*>>(a: ElementReference<A>, b: ElementReference<B>)
    : OneToOneRelation<A, B>(a, b) {
    override var probability: ProbabilityInfo? = null

    override val id: String
        get() = "componentOf_${a}_${b}"

    override val isDirected: Boolean
        get() = true

    override fun copy() = ComponentOf(a, b)
}
