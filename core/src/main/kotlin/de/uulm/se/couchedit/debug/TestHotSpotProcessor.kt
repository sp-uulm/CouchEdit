package de.uulm.se.couchedit.debug

import com.google.inject.Inject
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.hotspots.LineFractionHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.hotspot.TestHotSpotDefinition
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.util.extensions.ref

class TestHotSpotProcessor @Inject constructor(
        private val repository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory
) : Processor {
    private val diffApplicator = Applicator(this.repository, diffCollectionFactory)

    override fun consumes(): List<Class<out Element>> = listOf(GraphicObject::class.java)

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val appliedDiffs = this.diffApplicator.apply(diffs)

        val ret = this.diffCollectionFactory.createMutableTimedDiffCollection()

        for (diff in appliedDiffs) {
            if (diff is ElementAddDiff && diff.added is GraphicObject<*> && diff.added.shape is Line) {
                val rel = LineFractionHotSpotDefinition(diff.added.ref().asType(), 0.5)

                ret.mergeCollection(this.repository.store(rel))
            }
        }

        return ret
    }
}
