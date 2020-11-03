package de.uulm.se.couchedit.statecharts.processing

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement

/**
 * Processor that just stores the abstract syntax of StateCharts. This is useful for debugging.
 */
@ProcessorScoped
class AbstractSyntaxStoreProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(StateChartAbstractSyntaxElement::class.java)

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        applicator.apply(diffs)

        return diffCollectionFactory.createMutableTimedDiffCollection()
    }
}
