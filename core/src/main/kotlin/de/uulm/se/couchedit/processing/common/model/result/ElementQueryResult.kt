package de.uulm.se.couchedit.processing.common.model.result

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

/**
 * Convenience wrapper for a result of a query to a ModelRepository.
 *
 * Enables callers to quickly check containment and retrieve Elements from it both by using [ElementReference] as well
 * as IDs.
 */
open class ElementQueryResult<out T : Element>(private val elements: Map<String, T> = emptyMap()) : Map<String, T> by elements {
    inline operator fun <reified X : Element> get(ref: ElementReference<X>): X? = this[ref.id] as? X

    inline operator fun <reified X : Element> contains(ref: ElementReference<X>) = ref.id in this && this[ref.id] is X

    fun filterKeys(predicate: (String) -> Boolean): ElementQueryResult<T> {
        return ElementQueryResult(elements.filterKeys(predicate))
    }

    fun filterValues(predicate: (T) -> Boolean): ElementQueryResult<T> {
        return ElementQueryResult(elements.filterValues(predicate))
    }

    fun filter(predicate: (Map.Entry<String, T>) -> Boolean): ElementQueryResult<T> {
        return ElementQueryResult(elements.filter(predicate))
    }
}
