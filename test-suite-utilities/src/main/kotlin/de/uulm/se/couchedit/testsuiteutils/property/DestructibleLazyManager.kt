package de.uulm.se.couchedit.testsuiteutils.property

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DestructibleLazyManager {
    private val destructibleLazyList = mutableListOf<ResettableLazyDelegate<*>>()

    fun <T> getLazy(initializer: () -> T): DelegateProvider<T> {
        return DelegateProviderImpl(initializer)
    }

    fun destroyAll() {
        synchronized(destructibleLazyList) {
            for (property in destructibleLazyList) {
                property.reset()
            }
        }
    }

    private fun registerProperty(delegate: ResettableLazyDelegate<*>) {
        synchronized(destructibleLazyList) {
            destructibleLazyList.add(delegate)
        }
    }

    interface DelegateProvider<T> {
        operator fun provideDelegate(
                thisRef: CouchEditTest,
                prop: KProperty<*>
        ): ReadOnlyProperty<CouchEditTest, T>
    }

    private inner class DelegateProviderImpl<T>(
            val initializer: () -> T
    ) : DelegateProvider<T> {
        override operator fun provideDelegate(
                thisRef: CouchEditTest,
                prop: KProperty<*>
        ): ReadOnlyProperty<CouchEditTest, T> {
            val delegate = ResettableLazyDelegate(initializer)

            this@DestructibleLazyManager.registerProperty(delegate)

            return delegate
        }
    }

    /**
     * A property delegate that functions similarly to the stdlib [lazy], only that its value can be
     * unset by the destroy() function, which causes it to be recreated if used again.
     *
     * Attention: As this is currently meant purely for test purposes, it is NOT thread-safe
     */
    private class ResettableLazyDelegate<T>(private val initializer: () -> T) : ReadOnlyProperty<CouchEditTest, T> {
        private var internalValue: Any? = UNINITIALIZEDVALUE

        override fun getValue(thisRef: CouchEditTest, property: KProperty<*>): T {
            if (internalValue === UNINITIALIZEDVALUE) {
                internalValue = initializer()
            }

            @Suppress("UNCHECKED_CAST")
            return internalValue as T
        }

        fun reset() {
            this.internalValue = UNINITIALIZEDVALUE
        }

        private object UNINITIALIZEDVALUE
    }
}
