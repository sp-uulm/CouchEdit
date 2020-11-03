package de.uulm.se.couchedit.processing.attribute.factory

import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.base.Element

/**
 * Interface for services that can be used to generate [AttributeBag]s for a certain element type [T].
 */
interface SubAttributeBagFactory<T: Element> {
    /**
     * Returns the types (classes) of [AttributeBag] that are available for the [element] given.
     */
    fun availableBagTypes(element: T): Set<Class<out AttributeBag>>

    /**
     * Creates an [AttributeBag] of the given [bagClass] that is applicable for the given [element].
     *
     * Which [bagClass]es can be
     */
    fun createBag(bagClass: Class<out AttributeBag>, element: T): AttributeBag?
}
