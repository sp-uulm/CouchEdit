package de.uulm.se.couchedit.client.controller.canvas.gef.policy

import com.google.inject.Inject
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasCoordinator
import de.uulm.se.couchedit.client.controller.canvas.processing.GraphicObjectQueries
import de.uulm.se.couchedit.client.controller.workspace.EditorPaneController
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.MouseEvent
import org.eclipse.gef.mvc.fx.handlers.AbstractHandler
import org.eclipse.gef.mvc.fx.handlers.IOnClickHandler
import org.eclipse.gef.mvc.fx.models.HoverModel
import org.eclipse.gef.mvc.fx.parts.IContentPart
import org.eclipse.gef.mvc.fx.policies.DeletionPolicy
import tornadofx.*
import java.util.*

internal class ShowGraphicObjectContextMenuOnClickPolicy @Inject constructor(
        private val canvasCoordinator: CanvasCoordinator,
        private val queries: GraphicObjectQueries
) : AbstractHandler(), IOnClickHandler {

    override fun click(event: MouseEvent) {
        if (!event.isSecondaryButtonDown) {
            return
        }

        val menuItems = mutableListOf<MenuItem>()

        (host as? BasePart<*, *>)?.let { basePartHost ->
            val icons = FontAwesomeIconFactory.get()

            val inspectNodeItem = MenuItem("Inspect Shapes")
            icons.setIcon(inspectNodeItem, FontAwesomeIcon.BUG)
            inspectNodeItem.setOnAction {
                this.inspectShape()
            }
            menuItems.add(inspectNodeItem)

            val deleteNodeItem = MenuItem("Delete Node")
            icons.setIcon(deleteNodeItem, FontAwesomeIcon.TRASH)
            deleteNodeItem.setOnAction {
                this.deletePart()
            }
            menuItems.add(deleteNodeItem)

            val toBackgroundItem = MenuItem("To Background")
            icons.setIcon(toBackgroundItem, FontAwesomeIcon.BACKWARD)
            toBackgroundItem.setOnAction {
                this.toBackground()
            }
            menuItems.add(toBackgroundItem)

            val oneStepToBackItem = MenuItem("One step to back")
            icons.setIcon(oneStepToBackItem, FontAwesomeIcon.STEP_BACKWARD)
            oneStepToBackItem.setOnAction {
                this.oneStepToBack()
            }
            menuItems.add(oneStepToBackItem)

            val oneStepToFrontItem = MenuItem("One step to front")
            icons.setIcon(oneStepToFrontItem, FontAwesomeIcon.STEP_FORWARD)
            oneStepToFrontItem.setOnAction {
                this.oneStepToFront()
            }
            menuItems.add(oneStepToFrontItem)

            val toForegroundItem = MenuItem("To Foreground")
            icons.setIcon(toForegroundItem, FontAwesomeIcon.FORWARD)
            toForegroundItem.setOnAction {
                this.toForeground()
            }
            menuItems.add(toForegroundItem)

            val suggestions = buildSuggestionMenuItems(basePartHost)

            if (suggestions.isNotEmpty()) {
                menuItems += SeparatorMenuItem()
                menuItems.addAll(suggestions)
            }
        }

        val menu = ContextMenu()
        menu.items.addAll(menuItems)
        // show the menu at the mouse position
        menu.show(event.target as Node, event.screenX, event.screenY)
    }

    private fun buildSuggestionMenuItems(part: BasePart<*, *>): List<MenuItem> {
        return part.getContent()?.id?.let(this.queries::getSuggestionsFor)?.map { suggestion ->
            val ret = MenuItem(suggestion.title)
            ret.setOnAction {
                executeDiffCollection(suggestion.applyActions)
            }
            return@map ret
        } ?: emptyList()
    }

    private fun inspectShape() {
        val id = (host as? BasePart<*, *>)?.contentId ?: return

        val editorPaneController = getEditorPaneController()

        editorPaneController.inspectElement(id)
    }

    private fun deletePart() {
        // remove part from hover model (transient model, i.e. changes are not undoable)
        val hover = host.viewer.getAdapter(HoverModel::class.java)
        if (host === hover.hover) {
            hover.clearHover()
        }

        // query DeletionPolicy for the removal of the host
        val root = host.root
        val delPolicy = root.getAdapter(DeletionPolicy::class.java)
        init(delPolicy)

        // get all achoreds and check if we have a connection part
        for (a in ArrayList(host.anchoredsUnmodifiable)) {
        }

        // and finally remove the node part
        delPolicy.delete(host as IContentPart<out Node>)
        commit(delPolicy)
    }

    private fun toForeground() {
        val policy = this.getZOrderPolicy()
        init(policy)

        policy.toForeground()

        commit(policy)
    }

    private fun toBackground() {
        val policy = this.getZOrderPolicy()
        init(policy)

        policy.toBackground()

        commit(policy)
    }

    private fun oneStepToBack() {
        val policy = this.getZOrderPolicy()
        init(policy)

        policy.oneStepToBack()

        commit(policy)
    }

    private fun oneStepToFront() {
        val policy = this.getZOrderPolicy()
        init(policy)

        policy.oneStepToFront()

        commit(policy)
    }

    private fun getZOrderPolicy(): ZOrderPolicy {
        return host.getAdapter(ZOrderPolicy::class.java)
    }

    private fun hostAsBasePart(): BasePart<*, *>? {
        return host as? BasePart<*, *>
    }

    private fun executeDiffCollection(it: DiffCollection) {
        this.canvasCoordinator.applyAndPublishPreparedDiffCollection(it)
    }

    private fun getEditorPaneController(): EditorPaneController {
        return FX.find(EditorPaneController::class.java)
    }
}
