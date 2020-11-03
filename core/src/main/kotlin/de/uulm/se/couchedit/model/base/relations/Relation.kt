package de.uulm.se.couchedit.model.base.relations

import de.uulm.se.couchedit.model.base.AbstractElement
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

/**
 * Generic Relation class, allows a set of from / to elements instead of only one
 */
abstract class Relation<out A : Element, out B : Element>(
        val aSet: Set<ElementReference<A>>,
        val bSet: Set<ElementReference<B>>
) : AbstractElement() {
    abstract override fun copy(): Relation<A, B>

    /**
     * Specifies whether [aSet] and [bSet] can be used interchangeably, or if their order matters.
     */
    open val isDirected: Boolean = false

    override fun contentEquivalent(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Relation<*, *>

        if (aSet != other.aSet) return false
        if (bSet != other.bSet) return false

        return true
    }
}
