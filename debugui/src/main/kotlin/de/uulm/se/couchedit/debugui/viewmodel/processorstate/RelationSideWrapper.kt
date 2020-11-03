package de.uulm.se.couchedit.debugui.viewmodel.processorstate

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation

/**
 * Data object class for a edge leading to a "explicitly" painted relation.
 */
data class RelationSideWrapper(val side: String, val index: Int, val relationRef: ElementReference<Relation<*, *>>) {
    override fun toString(): String {
        return side
    }
}
