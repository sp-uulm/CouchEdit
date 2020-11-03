package de.uulm.se.couchedit.processing.common.services.datastore

import de.uulm.se.couchedit.model.base.Element
import java.util.*

class RandomUUIDGenerator: IdGenerator {
    override fun generate(clazz: Class<out Element>): String = UUID.randomUUID().toString()

}