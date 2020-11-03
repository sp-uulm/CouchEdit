package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.client.controller.canvas.PartRegistry
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasOperationWorker.UpdateSpec
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory

/**
 * This service is responsible for handling the changes made to the GEF [BasePart] instances.
 */
@Singleton
internal class CanvasOperationHandler @Inject constructor(
        private val modelRepository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val canvasCoordinator: CanvasCoordinator,
        private val partRegistry: PartRegistry,
        private val canvasOperationWorker: CanvasOperationWorker
) {
    /**
     * Whether this OperationHandler also accepts requests to publish the "staging" content of a Part.
     */
    var publishStaging = true

    /**
     * Adds the given GEF [part] to the relevant facilities of the frontend application and propagates that adding
     * to the rest of the system.
     *
     * Only if the [activate] flag is set, the part registered is also set to "active" (i.e. future updates will be
     * published as well) and its insertion will be published to the processing components.
     */
    fun registerPart(part: BasePart<*, *>, activate: Boolean) {
        this.partRegistry.putPart(part.contentId!!, part)

        if (activate) {
            canvasOperationWorker.triggerUpdate(part, UpdateSpec.InsertOrUpdate(false))
            this.partRegistry.activate(part.contentId!!)
        }
    }

    /**
     * Removes the given GEF [part] from the relevant facilities of the frontend application and propagates that removal
     * to the rest of the system.
     */
    fun removePart(part: BasePart<*, *>): Boolean {
        val contentId = part.contentId ?: return false

        var wasActive = false

        synchronized(partRegistry) {
            check(partRegistry[contentId] === part) { "Part for $contentId was not the part to be removed!" }

            wasActive = partRegistry.remove(contentId)
        }

        canvasOperationWorker.triggerUpdate(part, UpdateSpec.Remove)

        return wasActive
    }

    /**
     * Collectively updates the subject tree (consisting of a [part] and its corresponding model element and all
     * potentially dependent elements, i.e. anchoreds and children)
     * in the relevant storage facilities of the front-end application, then publishes the changes to the
     * rest of the application.
     */
    fun triggerContentUpdate(part: BasePart<*, *>, staging: Boolean) {
        if (staging && !this.publishStaging) {
            return
        }

        synchronized(partRegistry) {
            if (part.contentId?.let { partRegistry.contains(it) && partRegistry.isActive(it) } != true) {
                return
            }
        }

        canvasOperationWorker.triggerUpdate(part, UpdateSpec.InsertOrUpdate(staging))
    }

    /**
     * Inserts / updates the given [bags] along with the appropriate [relations] to the frontend's model repository,
     * then publishes them to the rest of the system.
     */
    fun updateAttributes(bags: Set<AttributeBag>, relations: Set<AttributesFor>) {
        val diffCollection = diffCollectionFactory.createMutableTimedDiffCollection()

        for (element in bags.union(relations)) {
            diffCollection.mergeCollection(this.modelRepository.store(element))
        }

        this.canvasCoordinator.changesToCanvas(diffCollection)

        this.canvasCoordinator.publishDiffs(diffCollection)
    }
}
