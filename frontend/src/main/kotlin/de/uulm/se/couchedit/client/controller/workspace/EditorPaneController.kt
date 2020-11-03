package de.uulm.se.couchedit.client.controller.workspace

import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasCoordinator
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasOperationHandler
import de.uulm.se.couchedit.debugui.view.element.ShapedElementView
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.util.extensions.ref
import javafx.beans.property.SimpleBooleanProperty
import org.eclipse.core.commands.ExecutionException
import org.eclipse.gef.mvc.fx.domain.HistoricizingDomain
import org.eclipse.gef.mvc.fx.domain.IDomain
import tornadofx.*

/**
 * The [de.uulm.se.couchedit.client.view.workspace.EditorPane] is the whole environment containing the Palette, the
 * GEF canvas and (later) the suggestions etc.
 *
 * This de.uulm.se.couchedit.persistence.controller is responsible for all actions that happen on the meta level, such as actions executed on the
 * toolbar.
 */
class EditorPaneController : Controller() {
    private val domain: IDomain by di()

    private val canvasOperationHandler: CanvasOperationHandler by di()

    private val canvasCoordinator: CanvasCoordinator by di()

    private val loadedFileController: LoadedFileController by inject()

    /**
     * GEF / Eclipse Domain containing e.g. the undo facilities.
     */
    private val historicizingDomain = domain as? HistoricizingDomain

    val saveableProperty = loadedFileController.saveable

    /**
     * Property specifying whether the Editor currently has something to undo
     */
    val undoableProperty = SimpleBooleanProperty(false)

    /**
     * Property specifying whether the Editor currently has something to redo
     */
    val redoableProperty = SimpleBooleanProperty(false)

    val publishStagingProperty = SimpleBooleanProperty(canvasOperationHandler.publishStaging).apply {
        this.addListener { _, _, value -> canvasOperationHandler.publishStaging = value }
    }

    init {
        historicizingDomain?.operationHistory?.addOperationHistoryListener { e ->
            val ctx = historicizingDomain.undoContext
            undoableProperty.set(e.history.canUndo(ctx))
            redoableProperty.set(e.history.canRedo(ctx))
        }
    }

    fun load(path: String) {
        this.loadedFileController.load(path)
    }

    fun save() {
        this.loadedFileController.save()
    }

    fun saveAs(path: String) {
        this.loadedFileController.saveAs(path)
    }

    /**
     * Reverts the last executed action on the undo stack.
     */
    fun undo() {
        try {
            historicizingDomain?.operationHistory?.undo(historicizingDomain.undoContext, null, null)
        } catch (e1: ExecutionException) {
            e1.printStackTrace()
        }
    }

    /**
     * Re-executes the last undone action.
     */
    fun redo() {
        try {
            historicizingDomain?.operationHistory?.redo(historicizingDomain.undoContext, null, null)
        } catch (e1: ExecutionException) {
            e1.printStackTrace()
        }
    }

    fun inspectElement(elementId: String) {
        val repo = this.canvasCoordinator.modelRepository

        val element = repo[elementId] as? ShapedElement<*> ?: return

        val view = find(ShapedElementView::class.java)

        view.modelRepository = repo
        view.shownElement = element.ref()

        view.openWindow()
    }
}
