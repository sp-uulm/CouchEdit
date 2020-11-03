package de.uulm.se.couchedit.model.attribute

import de.uulm.se.couchedit.model.Copyable

/**
 * Interface for everything that can be contained in an
 * [de.uulm.se.couchedit.model.attribute.elements.AttributeBag].
 *
 * Attributes can be referenced by an [AttributeReference].
 *
 * For Attributes to be serializable, there currently are limitations on the content of the Attribute:
 * * The Attribute value must itself be serializable (i.e. a "primitive" type, an Enum etc.)
 * * The Attribute value may not be null
 */
abstract class Attribute<T> : Copyable {
    protected var valueInternal: T? = null

    var value: T
        get() = valueInternal ?: getDefault().also { valueInternal = it }
        set(value) {
            require(isLegalValue(value)) { "\"$value\" is not a valid value for ${this::class.simpleName}" }

            valueInternal = value
        }

    abstract override fun copy(): Attribute<T>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Attribute<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    /**
     * Sets the Attribute value to the given [value] if [value] is of a correct type.
     * If not, an [IllegalArgumentException] is thrown.
     */
    fun setUnsafe(value: Any) {
        require(this.getValueClass().isAssignableFrom(value::class.java)) {
            "${this::class.simpleName} cannot be set to a value of type ${value::class.simpleName}"
        }

        @Suppress("UNCHECKED_CAST") // checked above
        this.value = value as T
    }

    /**
     * The default value of the Attribute.
     */
    abstract fun getDefault(): T

    /**
     * @return The [Class] of value that this Attribute accepts
     */
    abstract fun getValueClass(): Class<T>

    /**
     * @return Whether the given [newValue] may be inserted into the Attribute's [value].
     */
    open fun isLegalValue(newValue: T): Boolean {
        return true
    }
}
