package de.uulm.se.couchedit.export.context.services

interface SerializableIdMapper {
    /**
     * Returns the ID to be given to the [de.uulm.se.couchedit.export.model.SerializableElement] representing the
     * data object with the given [elementId].
     */
    fun getSerializableId(elementId: String): String
}
