package de.uulm.se.couchedit.export.context

import de.uulm.se.couchedit.export.context.services.SerializableIdMapper

/**
 * Parameter object to be passed through the entire convertToSerializable process, to contain global process
 * information.
 */
data class ToSerializableContext(val serializableIdMapper: SerializableIdMapper? = null)
