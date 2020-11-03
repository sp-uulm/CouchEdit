package de.uulm.se.couchedit.export.context.services

interface DataObjectIdMapper {
    /**
     * Returns the ID to be given to the data object represented by the
     * [de.uulm.se.couchedit.export.model.SerializableElement] with the given [serializableId].
     */
    fun getDataId(serializableId: String): String
}
