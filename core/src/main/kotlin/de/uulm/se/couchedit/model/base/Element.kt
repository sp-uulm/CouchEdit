package de.uulm.se.couchedit.model.base

import de.uulm.se.couchedit.model.Copyable
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo

/**
 * Base interface for all objects in the Model of the application.
 *
 * Everything is an Element that can be handled by one of the application component, be it a graphics primitive,
 * a relation between Elements or a part of abstract syntax with semantics attached.
 *
 * For the correct function of the system, implementors must provide a implementation of [equals] so that an Element
 * equals another iff the class and the ID are the same.
 */
interface Element : Copyable {
    val id: String

    /**
     * Probability with which this Element is valid considering the input from which it was generated.
     */
    var probability: ProbabilityInfo?

    /**
     * Deep clones this Element and all related Elements. Used so that different processing facilities can work
     * independently and thread-safe.
     */
    override fun copy(): Element

    /**
     * Checks all element properties but the ID.
     */
    fun equivalent(other: Any?): Boolean {
        return this.probability == (other as? Element)?.probability && this.contentEquivalent(other)
    }

    /**
     * Checks all element "content" properties (i.e. not the ID and probability).
     * This is needed as in some cases an element must be recognized as
     * "equal" even if its properties are different, as long as it has the same ID.
     */
    fun contentEquivalent(other: Any?): Boolean
}
