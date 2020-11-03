package de.uulm.se.couchedit.client.util.collections

import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import java.util.*

class GraphicObjectZOrderSetImpl : MutableGraphicObjectZOrderSet {
    private val zOrderComparator = object : Comparator<GraphicObject<*>> {
        /**
         * The Z-Order of [o1] is compared to the Z of [o2] by comparing their first distinct element (or, if all elements
         * are equal, the Z-Order is equal. Then use the insertion order of the elements to determine a total ordering
         * between equal z-orders).
         *
         * We use the Zs from the [storedZMap] so that no inconsistencies happen when just one element is updated.
         */
        override fun compare(o1: GraphicObject<*>, o2: GraphicObject<*>): Int {
            if (o1 == o2) {
                return 0
            }

            val zo1 = getOrPutStoredZ(o1)
            val zo2 = getOrPutStoredZ(o2).toMutableList() // copy list

            val iterator = zo2.iterator()

            val comparision = if (zo1 == zo2) {
                0
            } else {
                val zo1Dropped = getOrPutStoredZ(o1).dropWhile {
                    if (!iterator.hasNext()) {
                        return@dropWhile false
                    }

                    val ret = iterator.next() == it
                    if (ret) {
                        iterator.remove()
                    }

                    return@dropWhile ret
                }

                (zo1Dropped.firstOrNull() ?: 0).compareTo(zo2.firstOrNull() ?: 0)
            }

            // if two elements have the same z, sort them by their index.
            if (comparision == 0) {
                return elementMap.keys.indexOf(o1.id).compareTo(elementMap.keys.indexOf(o2.id))
            }

            return comparision
        }
    }

    /**
     * Internal Set of [GraphicObject]s sorted by their [GraphicObject.z] component.
     */
    private val values = TreeSet<GraphicObject<*>>(this.zOrderComparator)

    /**
     * Mapping from [GraphicObject] IDs (elements stored in this Set) to the Z Value with which they were inserted into
     * the set. Used to detect changes in Z Order when calling [refreshOrder]
     */
    private val storedZMap = mutableMapOf<String, List<Int>>()

    /**
     * Mapping from [GraphicObject<*>] IDs to the Element objects stored in this set. Used when calling [refreshOrder] to
     * know what [GraphicObject<*>]s may need updating.
     *
     * Use a [LinkedHashMap] so that we have something to compare two elements with otherwise equal Z-Order by
     * (the last inserted element is always in front for better UX)
     */
    private val elementMap = linkedMapOf<String, GraphicObject<*>>()

    override fun add(element: GraphicObject<*>) {
        if (!this.elementMap.containsKey(element.id)) {
            this.elementMap[element.id] = element
            this.storedZMap[element.id] = element.z.toList()
            this.values.add(element)
        } else {
            this.refreshOrder(element)
        }
    }

    override fun remove(element: Any?): Boolean {
        if (element !is GraphicObject<*>) {
            return false
        }

        val treeResult = this.values.remove(element)
        val zMapResult = this.storedZMap.remove(element.id) != null
        val elementMapResult = this.elementMap.remove(element.id) != null

        if (treeResult != zMapResult || treeResult != elementMapResult || zMapResult != elementMapResult) {
            throw IllegalStateException("Inconsistent state between internal Set, ZOrder List and Element")
        }

        return treeResult
    }

    override fun getElementBehind(element: GraphicObject<*>): GraphicObject<*>? {
        refreshOrder()

        var current: GraphicObject<*>? = element
        while (current != null) {
            current = this.values.lower(current)

            if (current?.z != element.z) {
                return current
            }
        }

        return null
    }

    override fun getElementInFrontOf(element: GraphicObject<*>): GraphicObject<*>? {
        refreshOrder()

        var current: GraphicObject<*>? = element
        while (current != null) {
            current = this.values.higher(current)

            if (current?.z != element.z) {
                return current
            }
        }

        return null
    }

    /**
     * Gets the stored element with the highest Z coordinate
     */
    override fun getForemostElement(): GraphicObject<*> {
        refreshOrder()

        return this.values.last()
    }

    /**
     * Gets the stored element with the lowest Z coordinate
     */
    override fun getBackmostElement(): GraphicObject<*> {
        refreshOrder()

        return this.values.first()
    }

    /**
     * @return Whether this Set contains the provided [element]
     */
    override fun contains(element: GraphicObject<*>): Boolean {
        return this.elementMap.contains(element.id)
    }

    override fun toList(): List<GraphicObject<*>> {
        refreshOrder()

        return values.toList()
    }

    /**
     * Updates the order of the [GraphicObject<*>]s inserted in this object to reflect their current [zOrder].
     */
    override fun refreshOrder() {
        for (element in elementMap.values) {
            refreshOrder(element)
        }
    }

    /**
     * Updates the order of a specific [element] inserted in this object to reflect their current [zOrder],
     * or if this element is not yet in this collection, adds it to the collection.
     */
    private fun refreshOrder(element: GraphicObject<*>) {
        if (!this.contains(element)) {
            this.add(element)
            return
        }

        if (element.z != this.storedZMap[element.id]) {
            this.values.remove(element)

            this.storedZMap[element.id] = element.z
            this.values.add(element)
        }
    }

    /**
     * Gets the currently stored [GraphicObject<*>.z] of the given [element] from the List of stored Z coordinates, or puts the current
     * one into the list if none is stored
     */
    private fun getOrPutStoredZ(element: GraphicObject<*>): List<Int> {
        return storedZMap.getOrPut(element.id, { element.z.toList() })
    }
}
