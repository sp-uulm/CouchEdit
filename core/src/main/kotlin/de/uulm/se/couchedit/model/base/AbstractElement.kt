package de.uulm.se.couchedit.model.base

/**
 * Default implementation of [equals] and [hashCode] for Elements.
 * If an Element class does not inherit from AbstractElement, the implementation must take care of implementing equals
 * and hashCode as given here.
 */
abstract class AbstractElement : Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return this.id == (other as? Element)?.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode() + 31 * id.hashCode()
    }
}
