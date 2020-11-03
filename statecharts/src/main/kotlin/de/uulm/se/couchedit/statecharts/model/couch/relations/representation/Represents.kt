package de.uulm.se.couchedit.statecharts.model.couch.relations.representation

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement

/**
 * Relation between a [GraphicObject] and some type of [StateChartAbstractSyntaxElement] to symbolize that [a] is the
 * Concrete Syntax representation of the [b]
 */
abstract class Represents<A: ShapedElement<*>, B: StateChartAbstractSyntaxElement>(a: ElementReference<A>, b: ElementReference<B>)
    : OneToOneRelation<A, B>(a, b) {
    override val isDirected: Boolean = true

    abstract override fun copy(): Represents<A, B>

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
}
