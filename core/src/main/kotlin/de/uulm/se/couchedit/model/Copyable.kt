package de.uulm.se.couchedit.model

/**
 * Interface for classes which are able to be deep (!) copied
 */
interface Copyable {
    /**
     * Returns a deep copy of this object, i.e. all associated instances should be distinct, but equal (and equivalent)
     */
    fun copy(): Copyable
}
