package de.uulm.se.couchedit.model.attribute.types

import de.uulm.se.couchedit.model.attribute.Attribute

/**
 * Attribute type defining a set of possible values, out of which the user has to choose exactly one.
 *
 * TODO: User Interface specification ("friendly" names, icons etc.)
 */
abstract class EnumAttribute<V>(): Attribute<V>() {
    /**
     * List of values that this attribute can accept
     */
    abstract fun getLegalValues(): List<V>

    override fun isLegalValue(newValue: V): Boolean {
        return newValue in getLegalValues()
    }
}
