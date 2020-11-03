package de.uulm.se.couchedit.model.containment

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

/**
 * Relation signaling that the [a] ShapedElement directly contains the [b] GraphicObject,
 * i.e. there exists no series of Include relations so that a Include x1, ... xn Include b with
 * length >= 2.
 */
class Contains(a: ElementReference<ShapedElement<*>>, b: ElementReference<GraphicObject<*>>)
    : OneToOneRelation<ShapedElement<*>, GraphicObject<*>>(a, b) {
    override fun copy(): OneToOneRelation<ShapedElement<*>, GraphicObject<*>> {
        return Contains(a, b)
    }

    override val id: String = this::class.java.simpleName + a.id + "_" + b.id

    override val isDirected = true

    // currently, Contains is always Explicit, as it is based on geometric facts.
    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
}
