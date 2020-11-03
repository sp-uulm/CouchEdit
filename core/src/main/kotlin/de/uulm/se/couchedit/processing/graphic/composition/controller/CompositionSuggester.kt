package de.uulm.se.couchedit.processing.graphic.composition.controller

import com.google.inject.Inject
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.suggestions.BaseSuggestion
import de.uulm.se.couchedit.model.base.suggestions.SuggestionFor
import de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.spatial.relations.Include
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.util.extensions.ref

class CompositionSuggester @Inject constructor(
        private val modelRepository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory
) : Processor {
    val applicator = Applicator(modelRepository, diffCollectionFactory)

    override fun consumes(): List<Class<out Element>> = listOf(GraphicObject::class.java, Include::class.java, ComponentOf::class.java)

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val resultingDiffs = applicator.apply(diffs)

        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        for (diff in resultingDiffs) {
            val element = diff.affected

            if (element is Include
                    && element.a.referencesType(GraphicObject::class.java)
                    && element.b.referencesType(GraphicObject::class.java)
            ) {
                if (diff is ElementAddDiff) {
                    if (isComponentOf(element.a.id, element.b.id)) {
                        continue
                    }

                    val graphicObjectA = element.a.asType<GraphicObject<*>>()
                    val graphicObjectB = element.b.asType<GraphicObject<*>>()

                    val aElement = this.modelRepository[graphicObjectA] ?: continue
                    val bElement = this.modelRepository[graphicObjectB] ?: continue

                    val suggestion = BaseSuggestion(
                            "suggest_component_${element.b}_${element.a}",
                            "Make this a component of other element",
                            generateActionDiffCollection(bElement, aElement)
                    )

                    val suggestionFor = SuggestionFor(suggestion.ref(), graphicObjectB)

                    ret.mergeCollection(modelRepository.store(suggestion))
                    ret.mergeCollection(modelRepository.store(suggestionFor))
                }

                if (diff is ElementRemoveDiff) {

                }
            }
        }

        return ret
    }

    private fun generateActionDiffCollection(component: GraphicObject<*>, composite: GraphicObject<*>): DiffCollection {
        val ret = diffCollectionFactory.createPreparedDiffCollection()

        ret.putDiff(ElementAddDiff(ComponentOf(component.ref(), composite.ref())))

        return ret
    }

    private fun isComponentOf(a: String, b: String) = this.modelRepository.getRelationsBetweenElements(a, b, ComponentOf::class.java).isNotEmpty()
}
