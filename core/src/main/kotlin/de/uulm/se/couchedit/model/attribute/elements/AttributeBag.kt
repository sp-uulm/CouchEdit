package de.uulm.se.couchedit.model.attribute.elements

import de.uulm.se.couchedit.model.attribute.Attribute
import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.base.AbstractElement

/**
 * An Element that contains properties belonging to one (or more) certain other Elements, which can be edited by the
 * user through the front-end.
 *
 * Important: Such attributes should only pertain to secondary information (such as dashed or solid lines, arrow ends, ...).
 *            They do not have influence on the internal spatial representation of the object.
 *
 * This is kept in a separate class (instead of in object properties) for multiple reasons:
 * * Changes should be kept as atomic as possible (i.e. no spatial re-evaluation if the line style changes)
 * * For some AttributeBag types (especially those belonging to the abstract syntax) multiple Elements can have the same
 *   AttributeBag element associated to them so that the properties of an abstract syntax instance can be manipulated at
 *   any one of these elements.
 *
 * Because of their similar nature and potentially many types, AttributeBags can be serialized and deserialized
 * automatically if they have a constructor with one String argument, which represents the ID of the AttributeBag.
 */
abstract class AttributeBag : AbstractElement() {
    private val values = HashMap<AttributeReference<*>, Attribute<*>>(this.getDefaults())

    val readOnlyValues: Map<AttributeReference<*>, Attribute<*>>
        get() = this.values.toMap()

    /**
     * Return whether this AttributeBag can accept [ref] as a key. This is true, if for one key in [possibleKeys]:
     * * The attrId is the same as that of [ref] and
     * * The type is the same as that of [ref] or a subtype.
     */
    fun canContain(ref: AttributeReference<*>): Boolean {
        val defaults = this.getDefaults()

        if (ref in defaults.keys) {
            return true
        }

        for (key in defaults.keys) {
            if (ref.attrId == key.attrId && key.referencesType(ref.type)) {
                return true
            }
        }

        return false
    }

    /**
     * Gets the [Attribute] instance currently assigned to the given [ref] in this Bag.
     * If the [ref] does not have any value associated to it, <code>null</code> is returned.
     */

    operator fun <A : Attribute<*>> get(ref: AttributeReference<A>): A? {
        var value = values[ref]

        if (value == null) {
            val key = this.values.keys.find { it.attrId == ref.attrId && it.referencesType(ref.type) } ?: return null

            value = values[key] ?: return null
        }

        if (!ref.referencesType(value::class.java)) {
            return null
        }

        @Suppress("UNCHECKED_CAST") // it is ensured by the set() function that no invalid value can be entered into the map
        return value as A
    }

    /**
     * Sets the given [ref] to the Attribute instance [value] provided.
     */
    operator fun set(ref: AttributeReference<*>, value: Attribute<*>) {
        if (!ref.referencesType(value::class.java)) {
            throw IllegalArgumentException("Cannot set a ${ref.type.simpleName} attribute to a ${value::class.java} value")
        }

        if (!this.values.containsKey(ref)) {
            return
        }

        this.values[ref] = value
    }

    /**
     * The content of two [AttributeBag]s is equivalent if they are:
     * * Of the same type
     * * Have the same [values] stored in them
     */
    override fun contentEquivalent(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeBag

        if (values != other.values) return false

        return true
    }

    /**
     * Sets the [values] of this [AttributeBag] to all [values] of the given [other] AttributeBag
     */
    protected fun setFrom(other: AttributeBag) {
        this.values.clear()

        for ((ref, value) in other.values) {
            if (!this.canContain(ref)) {
                throw IllegalArgumentException("Bag of type ${this::class.java} cannot contain attribute $ref!")
            }

            this.values[ref] = value.copy()
        }
    }

    /**
     * Default values of this AttributeBag, which will be automatically
     *
     * Keys of defaults are also the available keys, mapped to the types they should / must contain
     */
    abstract fun getDefaults(): Map<AttributeReference<*>, Attribute<*>>

    abstract override fun copy(): AttributeBag
}
