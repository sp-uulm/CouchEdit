package de.uulm.se.couchedit.export.converters.attributes

import com.google.inject.Inject
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.exceptions.IllegalPropertyValue
import de.uulm.se.couchedit.export.exceptions.InstantiationImpossible
import de.uulm.se.couchedit.export.exceptions.NoSuchConverterFromDataType
import de.uulm.se.couchedit.export.model.SerializableObject
import de.uulm.se.couchedit.export.model.attribute.SerAttribute
import de.uulm.se.couchedit.export.util.reflect.ClassToStringConverter
import de.uulm.se.couchedit.model.attribute.Attribute

class AttributeConverter @Inject constructor(
        private val classToStringConverter: ClassToStringConverter
) : AbstractConverter<Attribute<*>, SerAttribute>() {
    override fun toSerializable(element: Attribute<*>, context: ToSerializableContext): SerAttribute {
        // Try to serialize the value. If no converter can be found, use it as is
        val valueToUse = try {
            element.value?.let { aggregateConverter.convertToSerializable(it, context) }
        } catch (e: NoSuchConverterFromDataType) {
            element.value
        }

        return SerAttribute().apply {
            attributeClass = classToStringConverter.classToString(element::class.java)
            valueClass = classToStringConverter.classToString(element.getValueClass())
            value = valueToUse
        }
    }

    override fun fromSerializable(serializable: SerAttribute, context: FromSerializableContext): Attribute<*> {
        val attributeClassName = SerAttribute::attributeClass.getNotNull(serializable)
        val value = SerAttribute::value.getNotNull(serializable)

        val attributeClass = classToStringConverter.getClassFromString(attributeClassName, Attribute::class.java)
                ?: throw IllegalPropertyValue(
                        serializable::class,
                        Attribute::class,
                        SerAttribute::attributeClass,
                        attributeClassName,
                        "Expected Attribute subtype!"
                )

        val attributeConstructor = try {
            attributeClass.getConstructor()
        } catch (e: NoSuchMethodException) {
            throw InstantiationImpossible(serializable::class.java, attributeClass, "No-argument constructor not found")
        }

        // try to deserialize the value, if not possible use it directly.
        val valueToUse = if (value is SerializableObject) {
            aggregateConverter.convertFromSerializable(value, context)
        } else {
            value
        }

        return attributeConstructor.newInstance().apply {
            setUnsafe(valueToUse)
        }
    }
}
