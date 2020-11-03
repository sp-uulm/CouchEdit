package de.uulm.se.couchedit.export.converters

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.SerializableObject

/**
 * The AggregateConverter is the central registry for all type-specific converters, and therefore the central entry
 * point for the Export module.
 *
 * As the data model of CouchEdit is complex and uses Kotlin's "final by default" and constructor properties extensively,
 * problems occur when trying to serialize these data objects, because any Serialization library runs into problems with
 * non-default Constructors (or in the case of the first-party kotlinx Serialization, with constructors having
 * non-property arguments).
 *
 * Therefore, this data model, which is built with the intention to provide as much convenience as possible when using
 * it, leads to major inconvenience when it comes to (De)serialization.
 * In this case, it was decided to do manual serialization and deserialization. To keep this library-agnostic
 * (well, as long as that library does not need Annotations), this is not directly done with a library's (de)serialize
 * hooks, but instead the services in this package generate POJO / POKO instances which meet the requirements typically
 * required for serialization / deserialization:
 * * No-argument constructor
 * * Only mutable (nullable) properties
 * * Property types also fulfill these criteria.
 *
 * The "regular" CouchEdit model object is called a **data object** in this module, where the "simple" POJO
 * representation is called a **serializable object**.
 */
interface AggregateConverter {
    /**
     * Converts the given [SerializableObject]
     *
     * @throws de.uulm.se.couchedit.export.exceptions.ConvertToSerializableException
     */
    fun convertToSerializable(input: Any, context: ToSerializableContext): SerializableObject

    /**
     * @throws de.uulm.se.couchedit.export.exceptions.ConvertFromSerializableException
     */
    fun convertFromSerializable(input: SerializableObject, options: FromSerializableContext): Any


}
