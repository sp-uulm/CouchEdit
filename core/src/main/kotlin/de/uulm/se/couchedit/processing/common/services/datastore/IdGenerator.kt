package de.uulm.se.couchedit.processing.common.services.datastore

import de.uulm.se.couchedit.model.base.Element

/**
 * Responsible for generating unique IDs for model [Element]s.
 */
interface IdGenerator {
    /**
     * Generates an ID for a class of the given [clazz] type.
     */
    fun generate(clazz: Class<out Element>): String
}
