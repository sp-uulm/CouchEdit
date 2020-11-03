package de.uulm.se.couchedit.processing.common.services.datastore

import com.google.inject.Singleton
import de.uulm.se.couchedit.model.base.Element

@Singleton
class LinearIDGenerator: IdGenerator {
    private var counter = 0

    override fun generate(clazz: Class<out Element>): String {
        synchronized(this) {
            counter += 1
            return String.format("%010X", counter).reversed()
        }
    }

}
