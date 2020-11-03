package de.uulm.se.couchedit.export.exceptions

import de.uulm.se.couchedit.export.model.SerializableObject
import kotlin.reflect.KClass

class IncompatibleDataType(
        val sourceType: KClass<out SerializableObject>,
        val targetType: KClass<*>
) : ConvertFromSerializableException() {
    override val message = "IncompatibleDeserializationTypes: ${sourceType.qualifiedName} cannot be converted to data type" +
            "${targetType.qualifiedName}"
}
