package de.uulm.se.couchedit.debug

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.suggestions.BaseSuggestion
import de.uulm.se.couchedit.model.base.suggestions.SuggestionFor
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollectionImpl
import de.uulm.se.couchedit.processing.common.model.diffcollection.PreparedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.util.extensions.ref

@ProcessorScoped
class TestSuggestionProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(PrimitiveGraphicObject::class.java)

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = MutableTimedDiffCollectionImpl()

        val changes = this.applicator.apply(diffs)

        for (it in changes.diffs.values) {
            if (it is ElementAddDiff && it.added is PrimitiveGraphicObject<*>) {
                if(it.added.content is Line) {
                    val suggestion = this.generateTestAttributeSuggestion(it.added)
                    val suggestionFor = SuggestionFor(suggestion.ref(), it.added.ref())

                    ret.mergeCollection(modelRepository.store(suggestion))
                    ret.mergeCollection(modelRepository.store(suggestionFor))
                }

                if (it.added.content is Rectangular) {
                    val suggestion = this.generateTestSuggestion(it.added)
                    val suggestionFor = SuggestionFor(suggestion.ref(), it.added.ref())

                    ret.mergeCollection(modelRepository.store(suggestion))
                    ret.mergeCollection(modelRepository.store(suggestionFor))
                }
            }
        }
        return ret
    }

    private fun generateTestSuggestion(pgo: PrimitiveGraphicObject<*>): BaseSuggestion {
        val xCopy = pgo.copy()
        (xCopy.content as Rectangular).apply {
            x = 0.0
            y = 0.0
        }

        val diffs = PreparedDiffCollection()
        diffs.putDiff(ElementModifyDiff(pgo, xCopy))

        return BaseSuggestion(
                "test_" + pgo.id,
                "Reset to start",
                diffs
        )
    }

    private fun generateTestAttributeSuggestion(pgo: PrimitiveGraphicObject<*>): BaseSuggestion {
        val diffs = PreparedDiffCollection()

        val attributeBag = LineAttributes("attr_" + pgo.id)
        attributeBag[GraphicAttributeKeys.LINE_STYLE] = LineStyle(LineStyle.Option.DASHED)

        val attributesFor = AttributesFor(attributeBag.ref(), pgo.ref())

        diffs.putDiff(ElementAddDiff(attributeBag))
        diffs.putDiff(ElementAddDiff(attributesFor))

        return BaseSuggestion(
                "test_" + pgo.id,
                "Make dashed",
                diffs
        )
    }

}
