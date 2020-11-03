package de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement

/**
 * Specifies that the given [a] [ConnectionEnd] is also a possible start or end of a transition from/to [b].
 * The [role] property specifies whether the transition originates from or goes towards the [b] [StateElement]
 */
class TransitionEndPoint(
        a: ElementReference<ConnectionEnd<*, *>>,
        b: ElementReference<StateElement>,
        override var probability: ProbabilityInfo?,
        var role: Role
) : OneToOneRelation<ConnectionEnd<*, *>, StateElement>(a, b) {
    override fun copy() = TransitionEndPoint(a, b, probability, role)

    override fun toString(): String = "TransitionEndPoint(role=$role, connEnd=${a.id}, state=${b.id}, prob=$probability)"

    override val isDirected = true

    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && this.role == (other as? TransitionEndPoint)?.role
    }

    enum class Role {
        FROM,
        TO
    }
}
