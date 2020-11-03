package de.uulm.se.couchedit.export.util.reflect

import com.google.inject.Singleton

@Singleton
class ClassToStringConverter {
    fun <T> getClassFromString(str: String, supertype: Class<T>): Class<out T>? {
        val clazz = Class.forName(str)

        if (!supertype.isAssignableFrom(clazz)) {
            return null
        }

        return clazz as Class<out T>
    }

    fun classToString(clazz: Class<*>): String {
        return clazz.name
    }
}
