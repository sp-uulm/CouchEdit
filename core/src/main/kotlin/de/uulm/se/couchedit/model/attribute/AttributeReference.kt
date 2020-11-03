package de.uulm.se.couchedit.model.attribute

/**
 * References an attribute (that may be contained in an AttributeBag).
 *
 * TODO: Find a concept for internationalization, user interface specification etc.
 */
data class AttributeReference<A: Attribute<*>>(
    val attrId: String,
    val type: Class<out A>
) {
    /**
     * Returns true if the [Attribute] type referenced by this [AttributeReference] is the same as or a subtype of
     * the given [clazz]. (Similar to how <code>instanceof</code> / <code>is</code> works)
     */
    fun referencesType(clazz: Class<*>): Boolean {
        return clazz.isAssignableFrom(type)
    }

    /**
     * Reflection-checked cast to another type parameter of AttributeReference.
     *
     * If the given type [X] is no subtype of this [AttributeReference]'s ŧype, an [IllegalArgumentException]
     * is thrown.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified X: Attribute<*>> asType(): AttributeReference<X> {
        if(!this.referencesType(X::class.java)) {
            throw IllegalArgumentException("Attribute type $type is not equal or a subtype of ${X::class.java} !")
        }

        return this as AttributeReference<X>
    }

    override fun toString(): String = "Attr: ${type.simpleName} [\"$attrId\"]"
}
