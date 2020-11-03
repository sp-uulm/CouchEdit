package de.uulm.se.couchedit.export.exceptions

import de.uulm.se.couchedit.export.model.SerializableObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class IllegalPropertyValue(
        val sourceType: KClass<out SerializableObject>,
        val targetType: KClass<*>,
        val property: KProperty<*>,
        val illegalValue: Any,
        val reason: String = ""
) : ConvertFromSerializableException() {
    override val message: String? = "IllegalPropertyValue: Cannot convert $sourceType into $targetType: " +
            "Value $illegalValue is illegal for ${property.name} - $reason"
}
