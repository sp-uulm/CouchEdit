package de.uulm.se.couchedit.model.attribute.types

import de.uulm.se.couchedit.model.attribute.Attribute

/**
 * Attribute that takes a [Boolean] value.
 */
class BooleanAttribute(): Attribute<Boolean>() {
    override fun isLegalValue(newValue: Boolean): Boolean {
        return true
    }

    constructor(value: Boolean): this() {
        this.value = value
    }

    override fun copy(): BooleanAttribute {
        return BooleanAttribute(value)
    }

    override fun getDefault() = false

    override fun getValueClass() = Boolean::class.java
}
