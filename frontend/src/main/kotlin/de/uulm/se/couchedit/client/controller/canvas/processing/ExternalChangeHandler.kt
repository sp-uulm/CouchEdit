package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.common.collect.HashMultimap
import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseBendablePart
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasContentUpdater.Change.Update
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasContentUpdater.Command.UpdateCommand
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.queries.RelationGraphQueries
import de.uulm.se.couchedit.util.extensions.ref
import java.util.concurrent.CompletableFuture

/**
 * Responsible for handling changes made directly to the repository (i.e. not via the GEF Canvas).
 */
@Singleton
internal class ExternalChangeHandler @Inject constructor(
        private val modelRepository: ModelRepository,
        private val graphicObjectQueries: GraphicObjectQueries,
        private val relationGraphQueries: RelationGraphQueries,
        private val canvasContentUpdater: CanvasContentUpdater
) {
    /**
     * Enqueues the given [diffs] for handling as external changes to the application data model.
     *
     * @param diffs The DiffCollection containing the external operations to be executed on the canvas
     * @param activateCreatedParts Whether the parts created from the [diffs] should be automatically registered and
     *                             published to the rest of the system.
     */
    fun onExternalChanges(diffs: DiffCollection, activateCreatedParts: Boolean) {
        onExternalChangesInternal(diffs, null, activateCreatedParts)
    }

    /**
     * Enqueues the given [diffs] for handling as external changes to the application data model, then waits for them
     * to be executed, and returns the [CanvasContentUpdater.Result] representing the executed actions.
     *
     * @param diffs The DiffCollection containing the external operations to be executed on the canvas
     * @param activateCreatedParts Whether the parts created from the [diffs] should be automatically registered and
     *                             published to the rest of the system.
     */
    fun onExternalChangesSync(diffs: DiffCollection, activateCreatedParts: Boolean): CanvasContentUpdater.Result {
        val future = CompletableFuture<CanvasContentUpdater.Result>()

        this.onExternalChangesInternal(diffs, future, activateCreatedParts)

        return future.get()
    }

    private fun onExternalChangesInternal(
            diffs: DiffCollection,
            future: CompletableFuture<CanvasContentUpdater.Result>?,
            activateCreatedParts: Boolean
    ) {
        val commands = mutableMapOf<String, CanvasContentUpdater.Command>()

        loop@ for (diff in diffs) {
            when (val element = diff.affected) {
                is GraphicObject<*> -> {
                    when (diff) {
                        is ElementAddDiff -> {
                            // if a GraphicObject has been added, we set all updateable properties.

                            val cmd = getOrCreateUpdate(commands, element.id)

                            cmd.e = Update(element)
                            retrieveAndUpdateParent(element.id, cmd)
                            retrieveAndUpdateChildren(element.id, cmd)
                            retrieveAndUpdateAttributes(element.id, cmd)
                            retrieveAndUpdateAnchoreds(element.id, cmd)
                            retrieveAndUpdateAnchorages(element.id, cmd)
                        }
                        is ElementModifyDiff -> {
                            val cmd = getOrCreateUpdate(commands, element.id)

                            cmd.e = Update(element)
                        }
                        is ElementRemoveDiff -> {
                            commands[element.id] = CanvasContentUpdater.Command.RemoveCommand(element.id)
                        }
                    }
                }
                is ConnectionEnd<*, *> -> {
                    val cmd = getOrCreateUpdate(commands, element.a.id)

                    retrieveAndUpdateAnchorages(element.a.id, cmd)
                }
                is ComponentOf<*, *> -> {
                    val cmd = getOrCreateUpdate(commands, element.a.id)

                    retrieveAndUpdateParent(element.a.id, cmd)
                }
                is AttributesFor -> {
                    val cmd = getOrCreateUpdate(commands, element.b.id)

                    retrieveAndUpdateAttributes(element.b.id, cmd)
                }
                is AttributeBag -> {
                    if (modelRepository[element.id] == null) {
                        /*
                         * if the attributed Element has been deleted, we cannot read the associated Elements anymore.
                         * Thus, we have to wait for the diff that is concerned with the removal of the AttributesFor
                         * elements (as the relations must be automatically deleted along with the AttributeBag).
                         */

                        continue@loop
                    }

                    val attributedElements = relationGraphQueries.getElementsRelatedFrom(
                            element.ref(),
                            AttributesFor::class.java,
                            true
                    )

                    for (attributed in attributedElements) {
                        val cmd = getOrCreateUpdate(commands, attributed.id)

                        retrieveAndUpdateAttributes(attributed.id, cmd)
                    }
                }
            }
        }

        canvasContentUpdater.enqueueChanges(commands, future, activateCreatedParts)
    }

    private fun getOrCreateUpdate(map: MutableMap<String, CanvasContentUpdater.Command>, id: String): UpdateCommand {
        var current = map[id] as? UpdateCommand

        if (current == null) {
            current = UpdateCommand()

            map[id] = current
        }

        return current
    }

    private fun retrieveAndUpdateChildren(id: String, cmd: UpdateCommand) {
        cmd::children.setUpdateLazy {
            return@setUpdateLazy graphicObjectQueries.getContainedFor(id)
        }
    }

    private fun retrieveAndUpdateParent(id: String, cmd: UpdateCommand) {
        cmd::parent.setUpdateLazy {
            return@setUpdateLazy graphicObjectQueries.getContainerFor(id)
        }
    }

    private fun retrieveAndUpdateAttributes(id: String, cmd: UpdateCommand) {
        cmd::attributes.setUpdateLazy {
            val attributeBags = graphicObjectQueries.getAttributeBagsFor(id)

            val setMultimap = HashMultimap.create<Class<out AttributeBag>, AttributeBag>()
            for (bag in attributeBags) {
                setMultimap.put(bag::class.java, bag)
            }

            return@setUpdateLazy setMultimap
        }
    }

    private fun retrieveAndUpdateAnchoreds(id: String, cmd: UpdateCommand) {
        cmd::anchoreds.setUpdateLazy {
            val queryResult = graphicObjectQueries.getAttachedLines(id)

            val ret = HashMultimap.create<GraphicObject<*>, String>()
            for ((go, isEnd) in queryResult) {
                val role = BaseBendablePart.getRole(isEnd)

                ret.put(go, role)
            }

            return@setUpdateLazy ret
        }
    }

    private fun retrieveAndUpdateAnchorages(id: String, cmd: UpdateCommand) {
        cmd::anchorages.setUpdateLazy {
            val (startAnchorage, endAnchorage) = graphicObjectQueries.getConnectionEndsForLine(id)

            val ret = HashMultimap.create<GraphicObject<*>, String>()

            startAnchorage?.let { ret.put(it, BaseBendablePart.ANCHOR_ROLE_START) }
            endAnchorage?.let { ret.put(it, BaseBendablePart.ANCHOR_ROLE_END) }

            return@setUpdateLazy ret
        }
    }
}
