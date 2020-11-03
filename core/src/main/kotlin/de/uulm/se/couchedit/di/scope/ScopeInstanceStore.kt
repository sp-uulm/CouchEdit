package de.uulm.se.couchedit.di.scope

import com.google.inject.Key

/**
 * Map store which saves the instances that are shared within a Scope.
 *
 * See https://stackoverflow.com/questions/9942782/custom-guice-scope-or-a-better-approach
 */
class ScopeInstanceStore {
    private val objects = mutableMapOf<Key<*>, Any>()

    @Suppress("UNCHECKED_CAST") // Correct typing is guaranteed by the set method
    operator fun <T : Any> get(k: Key<T>) = objects[k] as T

    operator fun <T : Any> set(k: Key<T>, v: T) {
        objects[k] = v
    }
}
