package de.uulm.se.couchedit.util.extensions

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

/**
 * Convenience function to quickly get an element reference from any Element
 */
fun <E : Element> E.ref() = ElementReference(this.id, this::class.java)

/**
 * Given a [ElementReference], returns a correctly typed [Element] instance from a map from [ElementReference] to [Element]
 */
inline fun <reified E : Element> Map<ElementReference<*>, Element>.getTyped(ref: ElementReference<E>): E? {
    val ret = this[ref]

    if (ret != null && ret !is E) {
        throw IllegalStateException(
                "Element with ID ${ref.id} must have type ${E::class.java}, got ${this[ref]?.javaClass}")
    }

    return this[ref] as? E
}
