package de.uulm.se.couchedit.model.base

data class ElementReference<out T : Element>(val id: String, val type: Class<out T>) {
    /**
     * Returns true if the [Element] type referenced by this [ElementReference] is the same as or a subtype of
     * the given [clazz]. (Similar to how <code>instanceof</code> / <code>is</code> works)
     */
    fun referencesType(clazz: Class<*>): Boolean {
        return clazz.isAssignableFrom(type)
    }

    /**
     * Reflection-checked cast to another type parameter of ElementReference.
     *
     * If the given type [X] is no subtype of this [ElementReference]'s ŧype, an [IllegalArgumentException]
     * is thrown.
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalArgumentException::class)
    inline fun <reified X : Element> asType(): ElementReference<X> {
        if (!this.referencesType(X::class.java)) {
            throw IllegalArgumentException("$type is not equal or a subtype of ${X::class.java} !")
        }

        return this as ElementReference<X>
    }

    override fun toString(): String = "${type.simpleName} [\"$id\"]"
}
