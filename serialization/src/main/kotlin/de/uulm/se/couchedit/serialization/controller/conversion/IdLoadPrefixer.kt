package de.uulm.se.couchedit.serialization.controller.conversion

import de.uulm.se.couchedit.export.context.services.DataObjectIdMapper

class IdLoadPrefixer(private val commonPrefix: String) : DataObjectIdMapper {
    override fun getDataId(serializableId: String): String {
        return commonPrefix + serializableId
    }
}
