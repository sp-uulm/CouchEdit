package de.uulm.se.couchedit.systemtestutils.controller.processing

import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.controller.ProcessorModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.systemtestutils.controller.manager.TestModificationPortRegistry
import de.uulm.se.couchedit.systemtestutils.controller.processing.model.TrackableDiffCollectionWrapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureNanoTime

/**
 * [ProcessorModificationPort] that allows us to introspect its behavior and communicates with the [registry]:
 * * All processed DiffCollections are given to the [TestModificationPortRegistry.onProcessingFinish] method
 * * The execution time of the Processor can be read via the [getAndResetTime]
 */
class TestProcessorModificationPort(
        processor: Processor,
        diffCollectionFactory: DiffCollectionFactory,
        centralExecutor: ExecutorService,
        private val registry: TestModificationPortRegistry
) : ProcessorModificationPort(processor, diffCollectionFactory, centralExecutor) {
    override val id: String = processor::class.java.simpleName

    private var consumedDiffCollectionIds = mutableSetOf<String>()

    private var isBusy = false

    var executionTimeMs = 0.0
        private set

    override fun onIncomingDiffCollection(diffCollection: TimedDiffCollection) {
        if (diffCollection is TrackableDiffCollectionWrapper) {
            if (diffCollection.isEmpty()) {
                registry.onProcessingFinish(this.id, diffCollection.ids, null)
            } else {
                consumedDiffCollectionIds.addAll(diffCollection.ids)
            }
        }
    }

    override fun doFlush() {
        val (diffs, consumedDiffCollectionIds) = synchronized(this) {
            val diffCollectionIds = consumedDiffCollectionIds

            consumedDiffCollectionIds = mutableSetOf()

            val diffs = getAndResetNextDiffs()

            return@synchronized Pair(diffs, diffCollectionIds)
        }

        if (diffs.isNotEmpty()) {
            isBusy = true

            val result = doProcess(diffs)

            val wrapper = TrackableDiffCollectionWrapper(setOf(registry.getDiffCollectionId()), result)

            isBusy = false

            // need to do this before publishing so the diffs cannot get processed before they have been added to the
            // Registry.
            registry.onProcessingFinish(this.id, consumedDiffCollectionIds, wrapper)

            publishResult(wrapper)
        } else {
            registry.onProcessingFinish(this.id, consumedDiffCollectionIds, null)
        }

    }

    private fun doProcess(diffCollection: TimedDiffCollection): TimedDiffCollection {
        var result: TimedDiffCollection? = null

        val nanos = measureNanoTime {
            result = processor.process(diffCollection)
        }

        synchronized(executionTimeMs) {
            executionTimeMs += nanosToMs(nanos)
        }

        return result!!
    }

    /**
     * Gets the time (in milliseconds) that the [processor] has spent in its [Processor.process] method since the
     * last call to [getAndResetTime]
     */
    fun getAndResetTime(): Double {
        synchronized(executionTimeMs) {
            val ret = executionTimeMs

            executionTimeMs = 0.0

            return ret
        }
    }

    companion object {
        fun nanosToMs(nanos: Long): Double = nanos.toDouble() / 1000000

        const val WATCH_INTERVAL_SECONDS = 30
    }
}
