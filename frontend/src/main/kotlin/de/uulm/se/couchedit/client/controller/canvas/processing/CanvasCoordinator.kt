package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.client.controller.canvas.PartRegistry
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.PreparedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryRead
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Central Coordinator for all actions happening to [GraphicObject]s on the
 * [de.uulm.se.couchedit.client.view.canvas.GefCanvasView].
 *
 * The Coordinator is responsible for pushing such actions to the Processing facilities via its [CanvasModificationPort] [port]
 * as well as updating the Canvas elements according to [DiffCollection]s coming in on the [port].
 */
@Singleton
internal class CanvasCoordinator @Inject constructor(
        private val port: CanvasModificationPort,
        val modelRepository: ModelRepository,
        private val partRegistry: PartRegistry,
        private val applicator: Applicator,
        private val externalChangeHandler: ExternalChangeHandler
) : ModelRepositoryRead by modelRepository {
    init {
        this.port.addObserver(object : Observer<TimedDiffCollection> {
            override fun onComplete() {}

            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {}

            override fun onNext(t: TimedDiffCollection) {
                onIncomingDiffs(t)
            }
        })
    }

    fun getPartById(id: String): BasePart<*, *>? {
        return partRegistry[id]
    }

    /**
     * Publishes the given [diffs] to the rest of the system.
     */
    fun publishDiffs(diffs: TimedDiffCollection) {
        if (diffs.isEmpty()) {
            return
        }

        this.port.outputSubject.onNext(diffs)
    }

    /**
     * Reacts on a [DiffCollection] coming in from outside the system.
     *
     * This includes:
     * * Applying the received [diffs] to the frontend's [ModelRepository]
     *
     */
    fun onIncomingDiffs(diffs: DiffCollection): TimedDiffCollection {
        val applied = this.applicator.apply(diffs)

        this.changesToCanvas(applied)

        return applied
    }

    /**
     * Clears the entire repository and instead adds the contents of the given [diffCollection].
     */
    fun replaceRepositoryContentsWith(diffCollection: PreparedDiffCollection) {
        val results = this.modelRepository.clear().toMutable()

        results.mergeCollection(applicator.apply(diffCollection))

        val result = externalChangeHandler.onExternalChangesSync(results, false)

        publishDiffs(results)

        // Only after the diffs have been successfully published, accept further diffs.
        synchronized(partRegistry) {
            for (part in result.addedParts) {
                part.contentId?.let { partRegistry.activate(it) }
            }
        }
    }

    /**
     * Applies the given [diffs] to the model displayed on the canvas, then publishes the resulting
     * diffs to the rest of the system.
     */
    fun applyAndPublishPreparedDiffCollection(diffs: DiffCollection) {
        val toPublish = diffs.toMutable()

        toPublish.mergeCollection(this.onIncomingDiffs(diffs))

        publishDiffs(toPublish)
    }

    /**
     * Applies the given [diffs] to the visuals displayed on the editor canvas.
     */
    fun changesToCanvas(diffs: DiffCollection) {
        this.externalChangeHandler.onExternalChanges(diffs, true)
    }
}
