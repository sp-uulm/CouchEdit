package de.uulm.se.couchedit.export.converters

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.exceptions.NoSuchConverterFromDataType
import de.uulm.se.couchedit.export.exceptions.NoSuchConverterFromSerializableType
import de.uulm.se.couchedit.export.model.SerializableObject

/**
 * Aggregate converter implementation. This needs to be encapsulated behind the [AggregateConverter] interface as
 * else DI can't resolve circular references (which are needed because Converters may need to convert sub-elements of
 * their given inputs.
 *
 * Attention: It has to be ensured by constructor callers that the correct Converter instances are
 *
 * @param toSerializable Map from supported Data Object classes to the [Converter]s that support conversion of these
 *                       Data Objects to Serializable Objects.
 * @param fromSerializable Map from supported Serializable Object classes to the [Converter]s that support conversion of these
 */
@Singleton
internal class AggregateConverterImpl @Inject constructor(
        private val toSerializable: Map<Class<*>, @JvmSuppressWildcards Converter<*, *>>,
        private val fromSerializable: Map<Class<SerializableObject>, @JvmSuppressWildcards Converter<*, *>>
) : AggregateConverter {
    override fun convertToSerializable(
            input: Any,
            context: ToSerializableContext
    ): SerializableObject {
        val converter = this.getConverterFromDataType(input::class.java)
                ?: throw NoSuchConverterFromDataType(input::class.java)

        return converter.toSerializable(input, context)
    }

    override fun convertFromSerializable(
            input: SerializableObject,
            options: FromSerializableContext
    ): Any {
        val converter = this.getConverterFromSerializable(input::class.java)
                ?: throw NoSuchConverterFromSerializableType(input::class.java)

        return converter.fromSerializable(input, options)
    }

    private fun <T : Any> getConverterFromDataType(clazz: Class<T>): Converter<Any, *>? {
        val converter = toSerializable[clazz]
                ?: toSerializable.filterKeys { it.isAssignableFrom(clazz) }.values.firstOrNull()

        @Suppress("UNCHECKED_CAST") // must be ensured by the ExportModule
        return converter as? Converter<Any, *>

    }

    private fun <S : SerializableObject> getConverterFromSerializable(clazz: Class<S>): Converter<*, SerializableObject>? {

        val converter = fromSerializable.filterKeys { it.isAssignableFrom(clazz) }.values.firstOrNull()

        @Suppress("UNCHECKED_CAST") // must be ensured by the ExportModule
        return converter as? Converter<*, SerializableObject>
    }

}
