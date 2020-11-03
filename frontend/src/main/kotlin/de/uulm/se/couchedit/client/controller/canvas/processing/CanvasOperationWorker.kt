package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import org.threadly.concurrent.wrapper.limiter.ExecutorLimiter
import java.util.concurrent.ExecutorService

/**
 * Internal worker to take load off the JavaFX UI thread by handling store and publish operations on a background
 * worker thread taken from the [centralExecutor].
 */
@Singleton
internal class CanvasOperationWorker @Inject constructor(
        private val modelRepository: ModelRepository,
        private val canvasCoordinator: CanvasCoordinator,
        private val diffCollectionFactory: DiffCollectionFactory,
        @Named("centralExecutor") private val centralExecutor: ExecutorService
) {
    private val inputExecutor = ExecutorLimiter(centralExecutor, 1)

    private var updates = mutableMapOf<BasePart<*, *>, UpdateSpec>()

    private var hasEnqueuedTask = false

    /**
     * Enqueues this [part] for updating. If [remove] is true, it will be removed from the ModelRepository instead
     */
    fun triggerUpdate(part: BasePart<*, *>, spec: UpdateSpec) {
        synchronized(updates) {
            updates[part] = spec

            if (!this.hasEnqueuedTask) {
                this.inputExecutor.submit {
                    flush()
                }
            }

            this.hasEnqueuedTask = true
        }
    }

    /**
     * Execute a run, taking all Elements from the [updates] and storing them, then publishing
     */
    private fun flush() {
        val diffs = diffCollectionFactory.createMutableTimedDiffCollection()

        val toUpdate = synchronized(updates) {
            this.hasEnqueuedTask = false

            val tmp = updates

            updates = mutableMapOf()

            return@synchronized tmp
        }

        for ((part, update) in toUpdate) {
            when (update) {
                is UpdateSpec.InsertOrUpdate -> {
                    val subjects = part.getAllContentsInTree(update.staging)

                    for ((_, element) in subjects) {
                        diffs.mergeCollection(modelRepository.store(element))
                    }
                }
                is UpdateSpec.Remove -> {
                    part.contentId?.let { modelRepository.remove(it) }?.let { diffs.mergeCollection(it) }
                }
            }
        }

        canvasCoordinator.publishDiffs(diffs)
    }

    sealed class UpdateSpec {
        data class InsertOrUpdate(val staging: Boolean) : UpdateSpec()
        object Remove : UpdateSpec()
    }
}
