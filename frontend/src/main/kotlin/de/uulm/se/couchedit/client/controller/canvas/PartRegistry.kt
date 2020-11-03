package de.uulm.se.couchedit.client.controller.canvas

import com.google.inject.Singleton
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart

/**
 * Central registry for [BasePart]s, mapping them from the IDs of the GraphicObjects they belong to.
 *
 * Additionally, this Registry contains whether a part is "activated", i.e. whether it may publish
 * updates into the back-end.
 */
@Singleton
internal class PartRegistry {
    private val partMap = mutableMapOf<String, BasePart<*, *>>()

    private val activeParts = mutableSetOf<String>()

    operator fun get(id: String): BasePart<*, *>? {
        synchronized(this) {
            return this.partMap[id]
        }
    }

    operator fun contains(id: String): Boolean {
        synchronized(this) {
            return this.partMap.contains(id)
        }
    }

    /**
     * Returns whether the Part representing the GraphicObject with the given [id] is activated,
     * i.e. whether updates triggered by this part will be accepted currently.
     */
    fun isActive(id: String): Boolean {
        synchronized(this) {
            return id in activeParts
        }
    }

    /**
     * Sets the GraphicObject with the given ID "active", i.e.
     */
    fun activate(id: String) {
        synchronized(this) {
            check(id in partMap) { "Cannot activate a part that is not registered!" }

            activeParts.add(id)
        }
    }

    fun deactivate(id: String) {
        synchronized(this) {
            synchronized(this) {
                activeParts.remove(id)
            }
        }
    }

    /**
     * @param id The ID of the Element for which a part should be found
     * @param lazyInitialization Function creating the fitting part if it is not yet contained in this registry.
     * @return Pair of the part found / created and a flag that is <code>true</code> if the part has been created via
     *         [lazyInitialization] or <code>false</code> if it already existed.
     */
    fun getOrPut(id: String, lazyInitialization: () -> BasePart<*, *>): Pair<BasePart<*, *>, Boolean> {
        synchronized(this) {
            val existingPart = this.partMap[id]

            if (existingPart != null) {
                return Pair(existingPart, false)
            }

            val newPart = lazyInitialization()

            require(newPart.contentId == id) { "Cannot insert part with contentId = ${newPart.contentId} for ID $id" }

            this.partMap[id] = newPart

            return Pair(newPart, true)
        }
    }

    fun putPart(id: String, part: BasePart<*, *>) {
        synchronized(this) {
            this.partMap[id] = part
        }
    }

    fun remove(id: String): Boolean {
        synchronized(this) {
            val ret = this.activeParts.remove(id)
            this.partMap.remove(id)
            return ret
        }
    }

    fun <T> withLock(op: () -> T): T {
        synchronized(this) {
            return op()
        }
    }
}
