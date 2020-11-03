package de.uulm.se.couchedit.processing.common.controller

import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import java.util.concurrent.ExecutorService
import kotlin.system.measureNanoTime

class ProductionProcessorModificationPort(
        processor: Processor,
        diffCollectionFactory: DiffCollectionFactory,
        centralExecutor: ExecutorService
) : ProcessorModificationPort(processor, diffCollectionFactory, centralExecutor) {
    override fun doFlush() {
        val diffCollection = getAndResetNextDiffs()

        if (diffCollection.isNotEmpty()) {
            val result = doProcess(diffCollection)

            publishResult(result)
        }
    }

    private fun doProcess(diffCollection: TimedDiffCollection): TimedDiffCollection {
        var result: TimedDiffCollection? = null

        val nanoseconds = measureNanoTime {
            try {
                result = this.processor.process(diffCollection)
            }
            catch(e: Exception) {
                e.printStackTrace()
            }
        }

        if (result?.isNotEmpty() == true) {
            println("FLUSH ${processor::class.java.simpleName} took ${nanoseconds.toDouble() / 1000000} ms (diffs: ${result!!.size})")
        } else {
            println("EMPTY ${processor::class.java.simpleName} took ${nanoseconds.toDouble() / 1000000} ms")
        }

        return result!!
    }
}
