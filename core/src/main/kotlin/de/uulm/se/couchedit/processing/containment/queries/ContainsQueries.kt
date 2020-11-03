package de.uulm.se.couchedit.processing.containment.queries

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.containment.Contains
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.processing.common.repository.ModelRepository

/**
 * Service to browse the graph stored in a [ModelRepository] by its [Contains] relations.
 */
@ProcessorScoped
class ContainsQueries @Inject constructor(private val modelRepository: ModelRepository) {
    /**
     * Starting from the Element specified by [ref], follows the path of [Contains] relations upwards (i.e. checks the
     * Elements containing the [ref] Element, then checks the elements containing these Elements, ...) until a Element
     * is found for which the given [function] does not return null.
     * The mapping results for every first such Element in all ancestor lines are returned as a Set.
     *
     * @return Set of mapping results of all container references in the first level of ancestors where at least one of
     *         the GraphicObjects does not yield <code>null</code> from the [function].
     */
    fun <T> mapFirstContainersNotNull(
            ref: ElementReference<ShapedElement<*>>,
            function: (ElementReference<ShapedElement<*>>) -> T?
    ): Set<T> {
        val mappedValue = function(ref)

        // If we can get a value out of the current ElementReference, return that mapped value
        if (mappedValue != null) {
            return setOf(mappedValue)
        }


        // else, find all containers of the current element and try our luck there...
        val ret = mutableSetOf<T>()

        val containsRelations = this.modelRepository.getRelationsToElement(ref.id, Contains::class.java, false).values

        val nextLevel = containsRelations.map(Contains::a)

        for (containerReference in nextLevel) {
            if (containerReference.referencesType(ShapedElement::class.java)) {
                ret.addAll(mapFirstContainersNotNull(containerReference.asType(), function))
            }

        }

        return ret.toSet()
    }
}
