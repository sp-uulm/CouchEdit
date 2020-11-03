package de.uulm.se.couchedit.statecharts.model.couch.elements

import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo

/**
 * Represents an orthogonal state (aka region) within a parent state.
 */
class OrthogonalRegion(override val id: String) : StateContainer {
    override var name: String? = null

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    override fun copy() = OrthogonalRegion(id).also {
        it.probability = this.probability
        it.name = this.name
    }

    override fun contentEquivalent(other: Any?): Boolean {
        return other is OrthogonalRegion && this.name == other.name
    }
}
