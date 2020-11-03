package de.uulm.se.couchedit.processing.connection.controller

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.suggestions.BaseSuggestion
import de.uulm.se.couchedit.model.base.suggestions.SuggestionFor
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory

@ProcessorScoped
class GenericConnectionSuggester @Inject constructor(
        private val modelRepository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val applicator: Applicator
) : Processor {
    override fun consumes(): List<Class<out Element>> {
        return listOf(GraphicObject::class.java, ConnectionEnd::class.java)
    }

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val appliedDiffs = applicator.apply(diffs)

        for (diff in appliedDiffs) {
            val affected = diff.affected
            if (affected is ConnectionEnd<*, *>) {
                val suggestionId = "suggest_" + affected.id

                when (diff) {
                    is ElementAddDiff -> {
                        val action = diffCollectionFactory.createPreparedDiffCollection()

                        val after = affected.copy()
                        after.probability = ProbabilityInfo.Explicit

                        action.putDiff(ElementModifyDiff(affected, after))

                        ret.mergeCollection(this.modelRepository.store(
                                BaseSuggestion(
                                        suggestionId,
                                        "Create connection here",
                                        action
                                )
                        ))

                        ret.mergeCollection(this.modelRepository.store(
                                SuggestionFor(
                                        ElementReference(suggestionId, BaseSuggestion::class.java),
                                        affected.a as ElementReference<GraphicObject<*>>
                                )
                        ))

                        ret.mergeCollection(this.modelRepository.store(
                                SuggestionFor(
                                        ElementReference(suggestionId, BaseSuggestion::class.java),
                                        affected.b as ElementReference<GraphicObject<*>>
                                )
                        ))
                    }
                    is ElementRemoveDiff -> {
                        ret.mergeCollection(this.modelRepository.remove(suggestionId))
                    }
                }
            }
        }

        return ret
    }

}
