package de.uulm.se.couchedit.processing.common.repository

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

/**
 * Helper that calls a certain service function (i.e. a function taking an [ElementReference] and a map of all related
 * Elements) using the information obtainable from the given [modelRepository].
 */
@ProcessorScoped
class ServiceCaller @Inject constructor(private val modelRepository: ModelRepository) {
    /**
     * Applies the given [service] function to the element [ref] as given by the ModelRepository and all of its related
     * elements.
     *
     * @see [ModelRepository.getElementAndRelated]
     */
    fun <X : Element, R> call(ref: ElementReference<X>, service: (ElementReference<X>, Map<ElementReference<*>, Element>) -> R): R? {
        val elements = modelRepository.getElementAndRelated(ref)

        if(ref !in elements) {
            return null
        }

        return service(ref, elements)
    }
}
