package de.uulm.se.couchedit.processing.attribute.factory

import com.google.inject.Inject
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.base.Element

/**
 * Service for generating [AttributeBag]s for arbitrary [Element]s.
 */
class AggregateAttributeBagFactory @Inject constructor(
       /*
        * We need to suppress wildcards here as Guice otherwise cannot correctly connect this parameter to the
        * MapBinder from the CoreModule
        */
       private val subFactories: Map<Class<Element>, @JvmSuppressWildcards SubAttributeBagFactory<*>>
) {
    private val subClassCache = mutableMapOf<Class<out Element>, SubAttributeBagFactory<*>>()

    fun <T: Element> availableBagTypes(element: T): Set<Class<out AttributeBag>> {
        @Suppress("UNCHECKED_CAST") val clazz = element::class.java as Class<T>

        val fac = this.findApplicableFactory(clazz)

        return fac.availableBagTypes(element)
    }

    /**
     * Creates an [AttributeBag] of the given [bagClass] that is applicable for the given [element].
     *
     * If the given Element does not support [bagClass], then <code>null</code> is returned.
     */
    fun <T: Element> createBag(bagClass: Class<out AttributeBag>, element: T): AttributeBag? {
        @Suppress("UNCHECKED_CAST") val clazz = element::class.java as Class<T>

        val fac = this.findApplicableFactory(clazz)

        return fac.createBag(bagClass, element)
    }

    @Suppress("UNCHECKED_CAST") // ensured by CoreModule
    private fun <T: Element> findApplicableFactory(clazz: Class<T>): SubAttributeBagFactory<T> {
        var ret = subFactories.get<Class<out Element>, SubAttributeBagFactory<*>>(clazz)

        if(ret == null) {
            ret = subClassCache[clazz]
        }

        if(ret == null) {
            val retKey = subFactories.keys.find { it.isAssignableFrom(clazz) }

            ret = subFactories[retKey]
                    ?: throw IllegalArgumentException("Cannot find a suitable Attribute Bag factory for $clazz")

            subClassCache[clazz] = ret
        }

        return ret as SubAttributeBagFactory<T>
    }
}
