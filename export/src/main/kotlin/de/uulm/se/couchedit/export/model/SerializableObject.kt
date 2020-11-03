package de.uulm.se.couchedit.export.model

import java.io.Serializable

/**
 * Serializability is an issue here.
 * Every class that is to be serialized via means like Gson etc. has to have
 *
 * * Primary constructor empty (or just val / var arguments)
 * * No duplicate fields in the inheritance hierarchy (which is quite pointless for cases like Relation).
 *
 * For this to work with the normal Element classes, the CouchEdit data model would have to be polluted considerably and
 * constraints enforced by final variables would be lost (e.g. immutability of a Relation's endpoints).
 *
 * To get around this, the Elements that should be serializable must be duplicated in the inheritance hierarchy of
 * this marker interface. This means a substantial increase in boilerplate code, but the serialization aspects are decoupled from
 * the application's main data model (i.e. also future changes can be handled gracefully).
 */
interface SerializableObject : Serializable
