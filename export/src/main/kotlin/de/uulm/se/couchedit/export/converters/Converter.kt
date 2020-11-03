package de.uulm.se.couchedit.export.converters

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.SerializableObject

/**
 * Interface for a service that is able to convert a **data type** [T] into a **serializable type**
 */
interface Converter<T : Any, S : SerializableObject> {
    fun toSerializable(element: T, context: ToSerializableContext): S

    fun fromSerializable(serializable: S, context: FromSerializableContext): T
}
