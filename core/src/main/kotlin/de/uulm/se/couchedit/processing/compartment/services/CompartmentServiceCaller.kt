package de.uulm.se.couchedit.processing.compartment.services

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentElement
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.compartment.PotentialCompartment
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.util.extensions.getTyped
import de.uulm.se.couchedit.util.extensions.ref

/**
 * This class is only for internal usage in the compartment processing!
 */
@ProcessorScoped
class CompartmentServiceCaller @Inject constructor(private val modelRepository: ModelRepository) {
    /**
     * Applies the provided [service] function to the [element].
     *
     * If the [element] is a PotentialCompartment, then the [service] will be applied as if the PotentialCompartment's
     * HotSpotDefinition is a "real" HotSpotDefinition. Also, all related PotentialCompartments will also be replaced
     * with their HotSpotDefinitions.
     */
    fun <R> call(
            ref: ElementReference<CompartmentElement>,
            service: (ElementReference<CompartmentHotSpotDefinition>, Map<ElementReference<*>, Element>) -> R
    ): R? {
        val elementsRaw = modelRepository.getElementAndRelated(ref)

        if (ref !in elementsRaw) {
            return null
        }

        val refedElement = elementsRaw.getTyped(ref)

        val hsdRef = if (refedElement is PotentialCompartment) refedElement.hsd.ref()
        else (refedElement as CompartmentHotSpotDefinition).ref()

        val elements = mutableMapOf<ElementReference<*>, Element>()

        for ((elRef, element) in elementsRaw) {
            if (element !is PotentialCompartment) {
                elements[elRef] = element

                continue
            }

            elements[element.hsd.ref()] = element.hsd
        }

        return service(hsdRef, elements)
    }
}
