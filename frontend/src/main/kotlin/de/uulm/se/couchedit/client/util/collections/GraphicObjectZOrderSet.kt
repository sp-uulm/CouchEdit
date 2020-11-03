package de.uulm.se.couchedit.client.util.collections

import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

/**
 * Set that is able to maintain a collection of [GraphicObject<*>]s, sorted by their Z-components (foremost elements
 * come last in the collection).
 *
 * The elements' Z coordinate can be updated externally (by a reference to this object elsewhere in the system); the
 * [refreshOrder] method then makes sure that the correct order is maintained again.
 */
interface GraphicObjectZOrderSet {
    /**
     * Gets the element with the next lower Z coordinate than [element].
     *
     * If there are elements with the same Z coordinate like [element], those are skipped and the next element with
     * **distinct**, lower Z coordinate is returned
     */
    fun getElementBehind(element: GraphicObject<*>): GraphicObject<*>?

    /**
     * Gets the element with the next higher Z coordinate than [element].
     *
     * If there are elements with the same Z coordinate like [element], those are skipped and the next element with
     * **distinct**, higher Z coordinate is returned
     */
    fun getElementInFrontOf(element: GraphicObject<*>): GraphicObject<*>?

    /**
     * Gets the stored element with the highest Z coordinate
     */
    fun getForemostElement(): GraphicObject<*>

    /**
     * Gets the stored element with the lowest Z coordinate
     */
    fun getBackmostElement(): GraphicObject<*>

    /**
     * @return Whether this Set contains the provided [element]
     */
    fun contains(element: GraphicObject<*>): Boolean

    /**
     * Returns a [List] containing the elements of this Set ordered by their Z coordinates
     */
    fun toList(): List<GraphicObject<*>>

    /**
     * Updates the order of the [GraphicObject<*>]s inserted in this object to reflect their current Z-Order
     * (if the elements' Z coordinates have changed externally).
     */
    fun refreshOrder()

}
