package de.uulm.se.couchedit.model.base.suggestions

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation

/**
 * Decorator class wrapping another [OneToOneRelation] to indicate that the specified [relation] has been proposed, but is not
 * in effect yet.
 */
class PotentialRelation<out A : Element, out B : Element, out T : OneToOneRelation<A, B>>(val relation: T)
    : OneToOneRelation<A, B>(relation.a, relation.b) {
    override var probability: ProbabilityInfo? = null

    override val isDirected: Boolean get() = relation.isDirected

    override fun copy() = PotentialRelation(relation.copy())

    override val id: String get() = "potential_${relation.id}"

    /**
     * Type of the decorated Relation.
     */
    val type: Class<*> = relation.javaClass
}
