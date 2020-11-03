package de.uulm.se.couchedit.di.scope

import com.google.common.base.Preconditions.checkState
import com.google.common.collect.Maps
import com.google.inject.Key
import com.google.inject.Provider
import com.google.inject.Scope
import com.google.inject.Scopes

/**
 * Class from the Guice documentation, converted to Kotlin
 * https://github.com/google/guice/wiki/CustomScopes
 *
 * Scopes a single execution of a block of code. Apply this scope with a
 * try/finally block: <pre>`
 *
 * scope.enter();
 * try {
 * // explicitly seed some seed objects...
 * scope.seed(Key.get(SomeObject.class), someObject);
 * // create and access scoped objects
 * } finally {
 * scope.exit();
 * }
`</pre> *
 *
 * The scope can be initialized with one or more seed values by calling
 * `seed(key, value)` before the injector will be called upon to
 * provide for this key. A typical use is for a servlet filter to enter/exit the
 * scope, representing a Request Scope, and seed HttpServletRequest and
 * HttpServletResponse.  For each key inserted with seed(), you must include a
 * corresponding binding:
 * <pre>`
 * bind(key)
 * .toProvider(de.uulm.se.couchedit.di.scope.SimpleScope.<KeyClass>seededKeyProvider())
 * .in(ScopeAnnotation.class);
`</pre> *
 *
 * @author Jesse Wilson
 * @author Fedor Karpelevitch
 */
class SimpleScope : Scope {
    private val defaultValues = ThreadLocal<MutableMap<Key<*>, Any>>()

    private val values = ThreadLocal<MutableMap<Key<*>, Any>>()

    fun enter() {
        checkState(values.get() == null, "A scoping block is already in progress")
        values.set(Maps.newHashMap())
    }

    fun exit() {
        checkState(values.get() != null, "No scoping block in progress")
        values.remove()
    }

    fun <T : Any> seed(key: Key<T>, value: T) {
        val scopedObjects = getScopedObjectMap(key)
        checkState(
                !scopedObjects.containsKey(key), "A value for the key %s was already seeded " +
                "in this scope. Old value: %s New value: %s", key,
                scopedObjects[key], value
        )
        scopedObjects[key] = value
    }

    fun <T : Any> seed(clazz: Class<T>, value: T) {
        seed(Key.get(clazz), value)
    }

    override fun <T : Any> scope(key: Key<T>, unscoped: Provider<T>): Provider<T?> {
        return Provider {
            val scopedObjects = getScopedObjectMap(key)

            var current: T? = scopedObjects[key] as? T
            if (current == null && !scopedObjects.containsKey(key)) {
                current = unscoped.get()

                // don't remember proxies; these exist only to serve circular dependencies
                if (Scopes.isCircularProxy(current)) {
                    return@Provider current
                }

                scopedObjects[key] = current
            }
            current
        }
    }

    private fun <T> getScopedObjectMap(key: Key<T>): MutableMap<Key<*>, Any> {
        return values.get() ?: defaultValues.get() ?: run {
            val newValues = mutableMapOf<Key<*>, Any>()
            defaultValues.set(newValues)
            return newValues
        }
    }

    companion object {

        private val SEEDED_KEY_PROVIDER = Provider<Any> {
            throw IllegalStateException("If you got here then it means that" +
                    " your code asked for scoped object which should have been" +
                    " explicitly seeded in this scope by calling" +
                    " de.uulm.se.couchedit.di.scope.SimpleScope.seed(), but was not.")
        }

        /**
         * Returns a provider that always throws exception complaining that the object
         * in question must be seeded before it can be injected.
         *
         * @return typed provider
         */
        fun <T> seededKeyProvider(): Provider<T> {
            return SEEDED_KEY_PROVIDER as Provider<T>
        }
    }
}
