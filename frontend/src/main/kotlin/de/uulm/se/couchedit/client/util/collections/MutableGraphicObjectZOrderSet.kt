package de.uulm.se.couchedit.client.util.collections

import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

interface MutableGraphicObjectZOrderSet : GraphicObjectZOrderSet {
    /**
     * Adds the given [GraphicObject] to the Set with the correct Z-ordering
     */
    fun add(element: GraphicObject<*>)

    /**
     * Removes the given [element] from the collection
     * @return Whether the Element was contained in the collection or not
     */
    fun remove(element: Any?): Boolean
}
