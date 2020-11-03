package de.uulm.se.couchedit.export.context

import de.uulm.se.couchedit.export.context.services.DataObjectIdMapper

/**
 * Parameter object to be passed through the entire convertFromSerializable process, to contain global process
 * information.
 */
data class FromSerializableContext(val dataObjectIdMapper: DataObjectIdMapper? = null)
