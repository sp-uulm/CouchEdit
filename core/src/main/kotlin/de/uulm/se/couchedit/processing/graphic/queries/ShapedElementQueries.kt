package de.uulm.se.couchedit.processing.graphic.queries

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.util.extensions.ref
import java.util.*

@ProcessorScoped
class ShapedElementQueries @Inject constructor(private val modelRepository: ModelRepository) {
    /**
     * @param ref [ElementReference] that points to a [ShapedElement] for which all dependent ShapedElements should be
     *            found
     *
     * @return Set of all [ShapedElement]s which are dependent on the properties of [ref] (including the Element
     *         referenced by [ref] itself)
     */
    fun getDependent(ref: ElementReference<ShapedElement<*>>): Set<ShapedElement<*>> {
        val ret = mutableSetOf<ShapedElement<*>>()

        val visitedRefs = mutableSetOf<ElementReference<*>>()

        val elementQueue = LinkedList<ShapedElement<*>>()

        val startEl = modelRepository[ref] ?: return emptySet()

        elementQueue.addLast(startEl)

        while (elementQueue.isNotEmpty()) {
            val element = elementQueue.removeFirst()

            ret.add(element)

            // now fetch all depending elements that are not yet visited.
            val dependents = modelRepository.getRelationsAdjacentToElement(
                    element.id,
                    HotSpotDefinition::class.java,
                    true
            )

            for (dependent: ShapedElement<*> in dependents.values) {
                val dRef = dependent.ref()

                if (dRef !in visitedRefs) {
                    visitedRefs.add(dRef)
                    elementQueue.addLast(dependent)
                }
            }
        }

        return ret
    }
}
