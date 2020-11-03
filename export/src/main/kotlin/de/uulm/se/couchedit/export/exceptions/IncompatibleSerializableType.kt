package de.uulm.se.couchedit.export.exceptions

import de.uulm.se.couchedit.export.model.SerializableObject
import kotlin.reflect.KClass

class IncompatibleSerializableType(
        val sourceType: KClass<*>,
        val targetType: KClass<out SerializableObject>
) : ConvertFromSerializableException() {
    override val message = "IncompatibleSerializableType: ${sourceType.qualifiedName} cannot be converted to serializable type" +
            "${targetType.qualifiedName}"
}
