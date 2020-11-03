package de.uulm.se.couchedit.export.converters.basic

import com.google.inject.Inject
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.exceptions.IllegalPropertyValue
import de.uulm.se.couchedit.export.model.basic.SerEnum
import de.uulm.se.couchedit.export.util.reflect.ClassToStringConverter

class EnumConverter @Inject constructor(
        private val classToStringConverter: ClassToStringConverter
) : AbstractConverter<Enum<*>, SerEnum>() {
    override fun toSerializable(element: Enum<*>, context: ToSerializableContext): SerEnum {
        return SerEnum().apply {
            valueName = element.name
            valueOrdinal = element.ordinal
            enumClass = classToStringConverter.classToString(element::class.java)
        }
    }

    override fun fromSerializable(serializable: SerEnum, context: FromSerializableContext): Enum<*> {
        val enumClassName = SerEnum::enumClass.getNotNull(serializable)

        val enumClass = classToStringConverter.getClassFromString(enumClassName, Enum::class.java)
                ?: throw IllegalPropertyValue(
                        serializable::class,
                        Enum::class,
                        SerEnum::enumClass,
                        enumClassName,
                        "Expected an enum class"
                )

        val ordinal = SerEnum::valueOrdinal.getNotNull(serializable)
        val name = SerEnum::valueName.getNotNull(serializable)

        val value = enumClass.enumConstants?.get(ordinal) ?: throw IllegalPropertyValue(
                serializable::class,
                enumClass::class,
                SerEnum::valueOrdinal,
                ordinal,
                "Could not find enum constant with given ordinal in ${enumClass.simpleName}!"
        )

        if (value.name != name) {
            throw IllegalPropertyValue(
                    serializable::class,
                    enumClass::class,
                    SerEnum::valueName,
                    name,
                    "Constant with ordinal $ordinal in Enum ${enumClass.simpleName} has name ${value.name}," +
                            " expected $name! Maybe Enum values have been updated."
            )
        }

        return value
    }

}
