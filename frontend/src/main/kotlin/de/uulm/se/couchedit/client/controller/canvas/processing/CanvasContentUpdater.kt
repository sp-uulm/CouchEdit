package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import com.google.inject.Inject
import de.uulm.se.couchedit.client.controller.canvas.PartRegistry
import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.system.RootDrawingPart
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.CouchContentPolicy
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.CreationAndRegistrationPolicy
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.DeletionAndDeregistrationPolicy
import de.uulm.se.couchedit.client.util.gef.contentViewer
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.util.extensions.ref
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.subjects.ReplaySubject
import org.eclipse.gef.mvc.fx.domain.IDomain
import org.eclipse.gef.mvc.fx.parts.IContentPart
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KMutableProperty0

/**
 * Internal worker that is responsible for changing the canvas content programmatically from the JavaFX thread.
 */
@ProcessorScoped
internal class CanvasContentUpdater @Inject constructor(
        private val domain: IDomain,
        private val partRegistry: PartRegistry,
        private val attributeManager: CanvasAttributeManager
) {
    private val updateStreamSubject = ReplaySubject.create<QueueItem>()

    private val rootDrawingPart by lazy {
        rootPart.childrenUnmodifiable[0] as RootDrawingPart
    }

    private val rootPart by lazy {
        domain.contentViewer.rootPart
    }

    private val creationPolicy by lazy {
        rootPart.getAdapter(CreationAndRegistrationPolicy::class.java)
    }

    private val deletionPolicy by lazy {
        rootPart.getAdapter(DeletionAndDeregistrationPolicy::class.java)
    }

    init {
        this.updateStreamSubject.observeOn(JavaFxScheduler.platform()).subscribe { (changeCollection, future, activateCreatedParts) ->
            this.processChanges(changeCollection, future, activateCreatedParts)
        }
    }

    /**
     * Instructs this Updater to handle the given [changeCollection]
     *
     * @param changeCollection Mapping from Content IDs to the Command to be executed
     * @param future Optional Future that will be set to completed whenever the processing of the changes has been
     *               completed
     * @param activateCreatedParts If set to true, the Parts that were created will also be activated after all changes
     *                             have been processed, that means also that the additions will automatically be
     *                             published. If false, the activation and publication has to be done by the caller.
     */
    fun enqueueChanges(
            changeCollection: Map<String, Command>,
            future: CompletableFuture<Result>? = null,
            activateCreatedParts: Boolean = true
    ) {
        this.updateStreamSubject.onNext(QueueItem(changeCollection, future, activateCreatedParts))
    }

    private fun processChanges(
            changeCollection: Map<String, Command>,
            future: CompletableFuture<Result>?,
            activateCreatedParts: Boolean
    ) {
        val addedParts = mutableSetOf<BasePart<*, *>>()

        synchronized(partRegistry) {
            changeLoop@ for ((elementId, change) in changeCollection) {
                when (change) {
                    is Command.UpdateCommand -> {
                        performInsertOrUpdate(elementId, change)?.let(addedParts::add)
                    }
                    is Command.RemoveCommand -> {
                        removePartFromCanvas(elementId)
                    }
                }
            }
        }

        if (activateCreatedParts) {
            for (part in addedParts) {
                part.contentId?.let(this.partRegistry::activate)
            }
        }

        future?.complete(Result(addedParts))
    }

    private fun performInsertOrUpdate(elementId: String, change: Command.UpdateCommand): BasePart<*, *>? {
        val (part, wasCreated) = partRegistry.getOrPut(elementId) {
            // Create (and register) the part for this Element if no part exists.
            synchronized(creationPolicy) {
                creationPolicy.init()

                val parentPart = change.parent.require()?.id?.let(this.partRegistry::get) ?: rootDrawingPart
                val anchoreds = getAnchoredParts(change.anchoreds.require())

                val part = creationPolicy.create(
                        change.e.require().copy(),
                        parentPart,
                        -42,
                        anchoreds,
                        doFocus = false,
                        doSelect = false,
                        doActivate = false
                )

                creationPolicy.commit()

                return@getOrPut part as BasePart<*, *>
            }
        }

        if (!wasCreated) {
            /*
             * if the part has just been created, we don't need to update parent / anchorages as that is done
             * automatically during creation.
             */

            if (change.parent is Change.Update) {
                updateParent(part, change)
            }

            if (change.anchoreds is Change.Update) {
                updateAnchoreds(part, change)
            }

            if (change.e is Change.Update) {
                @Suppress("UNCHECKED_CAST") // FIXME?
                part.getContent()?.setFrom(change.e.require() as GraphicObject<in Shape>)

                part.refreshVisual()
            }
        }

        if (wasCreated || change.anchorages is Change.Update) {
            updateAnchorages(part, change)
        }

        if (wasCreated || change.attributes is Change.Update) {
            updateAttributes(part, change)
        }

        if (wasCreated || change.children is Change.Update) {
            updateChildren(part, change)
        }

        return part
    }

    private fun removePartFromCanvas(elementId: String) {
        val part = partRegistry[elementId]

        if (part?.getParent() == null) {
            return
        }

        synchronized(deletionPolicy) {
            deletionPolicy.init()

            deletionPolicy.delete(part)

            deletionPolicy.commit()
        }
    }

    private fun updateAttributes(part: BasePart<*, *>, change: Command.UpdateCommand) {
        val newAttributes = change.attributes.require()

        part.attributes.clear()

        part.attributes.putAll(newAttributes)

        part.doRefreshAttributes()

        val notificationSet = mutableSetOf<Pair<ElementReference<AttributeBag>, AttributeBag>>()
        for (bag in newAttributes.values()) {
            notificationSet.add(Pair(bag.ref(), bag))
        }

        attributeManager.onAttributesChanged(part.contentId!!, notificationSet)
    }

    private fun updateParent(part: BasePart<*, *>, change: Command.UpdateCommand) {
        val parent = change.parent.require()

        val parentPart = parent?.id?.let(this.partRegistry::get) as? ChildrenSupportingPart<*>

        // check correct parent
        if (part.getParent() != parentPart) {
            val contentPolicy = getContentPolicy(part)

            synchronized(contentPolicy) {
                contentPolicy.init()

                contentPolicy.changeParent(parentPart)

                contentPolicy.commit()
            }
        }
    }

    private fun updateChildren(part: BasePart<*, *>, change: Command.UpdateCommand) {
        val childrenToAdd = change.children.require().toMutableSet()

        val childrenToRemove = mutableSetOf<GraphicObject<*>>()

        val currentChildren = part.getContentChildrenUnmodifiable()

        for (child in currentChildren) {
            if (!childrenToAdd.remove(child)) {
                childrenToRemove.add(child as GraphicObject<*>)
            }
        }

        val contentPolicy = getContentPolicy(part)

        contentPolicy.init()

        for (child in childrenToRemove) {
            contentPolicy.removeContentChild(child)
        }

        for (child in childrenToAdd) {
            if (child.id in partRegistry) {
                continue
            }

            contentPolicy.addContentChild(child, -42)
        }

        contentPolicy.commit()
    }

    private fun updateAnchorages(part: BasePart<*, *>, change: Command.UpdateCommand) {
        val anchoragesToAdd = change.anchorages.require()

        val anchoragesToRemove = HashMultimap.create<GraphicObject<*>, String>()

        val currentAnchorages = part.getContentAnchoragesUnmodifiable()

        for ((anchorage, roles) in currentAnchorages.asMap()) {
            for (role in roles) {
                if (!anchoragesToAdd.remove(anchorage, role)) {
                    (anchorage as? GraphicObject<*>)?.let {
                        anchoragesToRemove.put(it, role)
                    }
                }
            }
        }

        val contentPolicy = this.getContentPolicy(part)

        synchronized(contentPolicy) {
            contentPolicy.init()

            for ((anchorage, role) in anchoragesToRemove.entries()) {
                contentPolicy.detachFromContentAnchorage(anchorage, role)
            }

            for ((anchorage, role) in anchoragesToAdd.entries()) {
                if (partRegistry[anchorage.id] == null) {
                    continue
                }

                contentPolicy.attachToContentAnchorage(anchorage, role)
            }

            contentPolicy.commit()
        }
    }

    private fun updateAnchoreds(anchoragePart: BasePart<*, *>, change: Command.UpdateCommand) {
        val anchoredsToRemove = HashMultimap.create<BasePart<*, *>, String>()

        val anchoredsToAdd = HashMultimap.create<BasePart<*, *>, String>()
        for ((contentAnchored, roles) in change.anchoreds.require().asMap()) {
            val anchoredPart = partRegistry[contentAnchored.id] ?: continue

            for (role in roles) {
                anchoredsToAdd.put(anchoredPart, role)
            }
        }

        val currentAnchoreds = anchoragePart.getAnchoredsUnmodifiable()
        for (anchored in currentAnchoreds) {
            if (anchored !is BasePart<*, *>) {
                // Don't touch non-CouchEdit Parts
                continue
            }

            /*
             * check what roles the anchors from [anchorage] to [part] have currently.
             */
            val roles = anchored.anchoragesUnmodifiable.get(anchoragePart)

            /*
             * Now remove all roles from the toAdd set that are already present
             */
            val specifiedRoles = roles.filter { anchoredsToAdd.remove(anchored, it) }

            /*
             * Mark all existing roles for removal that are not in the Command's specified roles for this anchored
             */
            for (role in roles - specifiedRoles) {
                anchoredsToRemove.put(anchored, role)
            }
        }

        // Now take care of removing all marked anchors from the anchorages
        for ((anchoredPart, roles) in anchoredsToRemove.asMap()) {
            val contentPolicy = getContentPolicy(anchoredPart)

            synchronized(contentPolicy) {
                contentPolicy.init()

                for (role in roles) {
                    contentPolicy.detachFromContentAnchorage(anchoragePart, role)
                }

                contentPolicy.commit()
            }
        }

        // Add new anchoreds
        for (toAdd in anchoredsToAdd.keySet()) {
            val contentPolicy = getContentPolicy(toAdd)

            synchronized(contentPolicy) {
                contentPolicy.init()

                for (role in anchoredsToAdd[toAdd]) {
                    contentPolicy.attachToContentAnchorage(anchoragePart.getContent(), role)
                }

                contentPolicy.commit()
            }
        }
    }

    private fun getContentPolicy(part: BasePart<*, *>) = part.getAdapter(CouchContentPolicy::class.java)

    /**
     * @param inputMap MultiMap of GraphicObjects to their Part's anchor roles.
     * @return Content Part Map
     */
    private fun getAnchoredParts(inputMap: SetMultimap<GraphicObject<*>, String>): SetMultimap<IContentPart<*>, String> {
        val ret = HashMultimap.create<IContentPart<*>, String>()

        for ((graphicObject, roles) in inputMap.asMap()) {
            // Ignore parts for which we don't yet have a registered part
            // => these will automatically detect the current part as their anchorage when they are inserted.
            val part = partRegistry[graphicObject.id] ?: continue

            for (role in roles) {
                ret.put(part, role)
            }
        }

        return ret
    }

    sealed class Command {
        /**
         * @param e
         * @param parent
         * @param attributes
         * @param anchoreds Mapping from [BasePart]s that are anchored to this Element to their assigned
         *                  role strings.
         * @param anchorages Mapping from [GraphicObject]s which this Element is anchored to to their
         *                   assigned role strings.
         */
        class UpdateCommand(
                var e: Change<GraphicObject<*>> = Change.NoChange(),
                var parent: Change<GraphicObject<*>?> = Change.NoChange(),
                var children: Change<Set<GraphicObject<*>>> = Change.NoChange(),
                var attributes: Change<SetMultimap<Class<out AttributeBag>, AttributeBag>> = Change.NoChange(),
                var anchoreds: Change<SetMultimap<GraphicObject<*>, String>> = Change.NoChange(),
                var anchorages: Change<SetMultimap<GraphicObject<*>, String>> = Change.NoChange()
        ) : Command()

        class RemoveCommand(val id: String) : Command()
    }

    sealed class Change<T> {
        abstract fun require(): T

        class Update<T>(private val newValue: T) : Change<T>() {
            override fun require() = newValue
        }

        class NoChange<T> : Change<T>() {
            override fun require(): T {
                throw Exception("NoChange not allowed here!")
            }
        }
    }

    private data class QueueItem(
            val changes: Map<String, Command>,
            val future: CompletableFuture<Result>?,
            val activateCreatedParts: Boolean
    )

    /**
     * Result from processing a set of changes.
     */
    data class Result(val addedParts: Set<BasePart<*, *>>)
}

/**
 * If this Property already contains an Update change, this is a No-Op.
 * Else, the return value of [updateSource] will be used in an Update change
 */
internal fun <T> KMutableProperty0<CanvasContentUpdater.Change<T>>.setUpdateLazy(updateSource: () -> T) {
    if (this.get() is CanvasContentUpdater.Change.NoChange) {
        val content = updateSource()

        this.set(CanvasContentUpdater.Change.Update(content))
    }
}
