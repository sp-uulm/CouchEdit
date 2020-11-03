package de.uulm.se.couchedit.export.util.couch

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.SerializableElement
import de.uulm.se.couchedit.model.base.Element


/**
 * Gets the adequate ID for the [SerializableElement] representing the [Element] with the receiver ID, as specified by
 * the [context].
 */
internal fun String.getSerializableId(context: ToSerializableContext): String {
    return context.serializableIdMapper?.getSerializableId(this) ?: this
}

/**
 * Gets the adequate ID for the [Element] represented by the [SerializableElement] with the receiver ID,
 * as specified by the [context].
 */
internal fun String.getDataId(context: FromSerializableContext): String {
    return context.dataObjectIdMapper?.getDataId(this) ?: this
}
