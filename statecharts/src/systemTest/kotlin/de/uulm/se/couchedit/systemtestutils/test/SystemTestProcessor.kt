package de.uulm.se.couchedit.systemtestutils.test

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.RelationCache
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.common.services.diff.VersionManager

/**
 * A simple Processor implementation that simply consumes the Element types specified by its [consumes] constructor
 * parameter and allows access to its [modelRepository] for assertion purposes
 */
class SystemTestProcessor(private val consumes: List<Class<out Element>>) : Processor {
    private val diffCollectionFactory = DiffCollectionFactory()

    val modelRepository = RootGraphBasedModelRepository(
            diffCollectionFactory,
            RelationCache(),
            VersionManager("_systemTestProcessor")
    )

    val applicator = Applicator(modelRepository, diffCollectionFactory)

    override fun consumes(): List<Class<out Element>> = consumes

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val output = diffCollectionFactory.createMutableTimedDiffCollection()

        applicator.apply(diffs, Applicator.ParallelStrategy.IGNORE)

        return output
    }
}
