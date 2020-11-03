package de.uulm.se.couchedit.export.converters

import com.google.inject.Inject
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.exceptions.IncompatibleDataType
import de.uulm.se.couchedit.export.exceptions.IncompatibleSerializableType
import de.uulm.se.couchedit.export.exceptions.RequiredPropertyMissing
import de.uulm.se.couchedit.export.model.SerializableObject
import kotlin.reflect.KProperty1

/**
 * Base class for implementation of ordinary [Converter]s.
 * Provides utility functions to work with nested serialized objects.
 */
abstract class AbstractConverter<T : Any, S : SerializableObject> : Converter<T, S> {
    /**
     * Link back to the general [AggregateConverter], which can be used to serialize / deserialize any nested objects.
     */
    protected lateinit var aggregateConverter: AggregateConverter
        private set

    /**
     * Setter for DI. This is not done by constructor injection as otherwise the dependency would have to be repeated
     * in every subclass' constructor.
     */
    @Inject
    fun setAggregateConverter(aggregateConverter: AggregateConverter) {
        this.aggregateConverter = aggregateConverter
    }

    /**
     * Helper function that safely reads the receiver property's value from the given serialized [serValue] [SerializableObject].
     * If the value is not null, it is returned, else a [RequiredPropertyMissing] exception is thrown automatically.
     */
    protected fun <I : SerializableObject, R : Any> KProperty1<I, R?>.getNotNull(serValue: I): R {
        return this.get(serValue) ?: throw RequiredPropertyMissing(this, serValue)
    }

    /**
     * Reads the receiver property from the given [serValue] [SerializableObject].
     *
     * If it is null, a [RequiredPropertyMissing] exception will be thrown, else it is attempted to convert the
     * read value into an [R] instance.
     *
     * If the converted value is an [R] instance, it is returned, else a [IncompatibleDataType] Exception is thrown.
     *
     * @return Data type representation of the receiver value (which is a serialized value)
     * @throws RequiredPropertyMissing If the receiver property is null for [serValue]
     * @throws IncompatibleDataType If the deserialized value of the receiver property is not an [R] object.
     */
    protected inline fun <reified V : SerializableObject, reified R : Any> KProperty1<S, V?>.fromSerializableNotNull(
            serValue: S,
            context: FromSerializableContext
    ): R {
        val serializedSubValue = this.get(serValue) ?: throw RequiredPropertyMissing(this, serValue)

        return checkedConvertFromSerializable(serializedSubValue, context)
    }

    /**
     * Reads the receiver property (which contains a list of [SerializableObject]s) from the given
     * [serValue] [SerializableObject], then maps the read list to its data type representation.
     */
    protected inline fun <reified V : SerializableObject, reified R : Any> KProperty1<S, List<V>?>.fromSerializableListNotNull(
            serValue: S,
            context: FromSerializableContext
    ): List<R> {
        val serializedListValue = this.get(serValue) ?: throw RequiredPropertyMissing(this, serValue)

        return serializedListValue.map {
            checkedConvertFromSerializable<R>(it, context)
        }
    }

    /**
     * Converts the given [input] object into a [R] (serializable type) instance.
     *
     * @param input Data type value
     * @return Serializable type [R] representation of the [input] value.
     * @throws IncompatibleSerializableType If the serialized version of the [input] value is not an [R]
     */
    protected inline fun <reified R : SerializableObject> checkedConvertToSerializable(
            input: Any,
            context: ToSerializableContext
    ): R {
        return aggregateConverter.convertToSerializable(input, context) as? R
                ?: throw IncompatibleSerializableType(input::class, R::class)
    }

    /**
     * Converts the given [input] object into a [R] (data type) instance.
     *
     * @param input Serializable type value
     * @return Data type [R] representation of the [input] value.
     */
    protected inline fun <reified R : Any> checkedConvertFromSerializable(input: SerializableObject, context: FromSerializableContext): R {
        return aggregateConverter.convertFromSerializable(input, context) as? R
                ?: throw IncompatibleDataType(input::class, R::class)
    }
}
