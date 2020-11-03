package de.uulm.se.couchedit.export.exceptions

import de.uulm.se.couchedit.export.model.SerializableElement
import de.uulm.se.couchedit.export.model.SerializableObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

data class RequiredPropertyMissing(
        val property: KProperty<*>,
        val serialized: SerializableObject
) : ConvertFromSerializableException() {
    val sourceClass: KClass<out SerializableObject> = serialized::class

    override fun toString(): String {
        return ("RequiredPropertyMissing: ${property.name} required for deserialization from $sourceClass"
                + if (serialized is SerializableElement) ", missing for ID ${serialized.id} " else "")
    }
}
