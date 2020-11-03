package de.uulm.se.couchedit.statecharts.model.couch.elements

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo

/**
 * Represents a "regular" state in the StateChart abstract syntax.
 */
class State(
        override val id: String,
        name: String? = null,
        var isSubmachineState: Boolean = false
) : StateElement(name), StateContainer {
    override fun copy(): Element = State(id, name, isSubmachineState)

    override fun contentEquivalent(other: Any?): Boolean {
        if (other !is State) {
            return false
        }

        return other.name == this.name && other.isSubmachineState == this.isSubmachineState
    }

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    override fun toString(): String = "State(id=$id, name=$name, submachine=$isSubmachineState)"
}
