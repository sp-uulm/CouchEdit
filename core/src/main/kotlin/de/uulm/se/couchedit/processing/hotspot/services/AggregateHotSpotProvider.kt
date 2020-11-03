package de.uulm.se.couchedit.processing.hotspot.services

import com.google.inject.Inject
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition

/**
 * Service that holds a DI'd Map of HotSpotProviders able to handle different kinds of HotSpotDefinitions and get their
 * Shapes.
 *
 * @param
 */
internal class AggregateHotSpotProvider @Inject constructor(
        /*
         * We need to suppress wildcards here as Guice otherwise cannot correctly connect this parameter to the
         * MapBinder from the CoreModule
         */
        private val subGenerators: Map<Class<HotSpotDefinition<*>>, @JvmSuppressWildcards HotSpotProvider<*, *>>
) {

    /**
     * Generates a [Shape] by the given [hRef] pointing to a HotSpotProvider and its [related] elements,
     * using the appropriate service from [subGenerators].
     */
    /*
    * Suppress needed for casting the HotSpotProvider.
    * The DI module must make sure that any Class<HotSpotDefinition> in subGenerators is mapped to a HotSpotProvider
    * that can handle it
    */
    @Suppress("UNCHECKED_CAST")
    fun <S: Shape, H : HotSpotDefinition<S>> generateShape(
            hRef: ElementReference<H>,
            elements: Map<ElementReference<*>, Element>
    ): S? {
        /*
         * TODO: Handle class hierarchies of HotSpotDefinitions correctly, i.e. if there is no HotSpotProvider for the
         *       exact class, use the one for the most-specific superclass.
         */

        val definitionClass = elements.getValue(hRef)::class.java as Class<HotSpotDefinition<*>>

        val provider = this.subGenerators[definitionClass] as? HotSpotProvider<S, H>
                ?: throw RuntimeException("No HotSpotProvider available for HotSpotDefinition of type $definitionClass.")

        return provider.generateShape(hRef, elements)
    }
}
