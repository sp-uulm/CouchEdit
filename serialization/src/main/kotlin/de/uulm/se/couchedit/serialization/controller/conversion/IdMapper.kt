package de.uulm.se.couchedit.serialization.controller.conversion

import de.uulm.se.couchedit.export.context.services.SerializableIdMapper

class IdMapper(private val commonPrefix: String) : SerializableIdMapper {
    var counter: Long = 0

    /**
     * Map from Data Object ID to Serializable ID.
     */
    val map = mutableMapOf<String, String>()

    override fun getSerializableId(elementId: String): String {
        return map.getOrPut(elementId) { newId() }
    }

    private fun newId(): String {
        synchronized(counter) {
            counter++
            return commonPrefix + "_" + counter.toString(16)
        }
    }
}
