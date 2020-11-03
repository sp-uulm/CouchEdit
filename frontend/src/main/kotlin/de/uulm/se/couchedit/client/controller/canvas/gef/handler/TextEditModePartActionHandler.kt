package de.uulm.se.couchedit.client.controller.canvas.gef.handler

import de.uulm.se.couchedit.client.controller.canvas.gef.part.TextEditModePart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.InplaceTextManipulationModel
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import org.eclipse.gef.mvc.fx.domain.IDomain
import org.eclipse.gef.mvc.fx.handlers.AbstractHandler
import org.eclipse.gef.mvc.fx.handlers.IOnClickHandler
import org.eclipse.gef.mvc.fx.handlers.IOnStrokeHandler
import org.eclipse.gef.mvc.fx.models.HoverModel
import org.eclipse.gef.mvc.fx.models.SelectionModel

/**
 * Handler responsible for executing appropriate actions when clicking inside [TextEditModePart]s (to start editing)
 * or pressing keys while those parts are selected (to start editing).
 */
class TextEditModePartActionHandler : AbstractHandler(), IOnStrokeHandler, IOnClickHandler {
    private val hoverModel by lazy {
        host.root.viewer.getAdapter(HoverModel::class.java)
    }

    private val selectionModel by lazy {
        host.root.viewer.getAdapter(SelectionModel::class.java)
    }

    /**
     * The Model in which the current Text manipulation state is stored (if / which [TextEditModePart] is currently
     * active and being edited)
     */
    private val textManipulationModel by lazy {
        host.root.viewer.getAdapter(InplaceTextManipulationModel::class.java)
                ?: throw IllegalStateException("No InplaceTextManipulationModel bound to current viewer")
    }

    /**
     * * Double clicking in a text field starts the editing process in that text field
     * * Clicking in another Part to which this action is associated commits currently running edit operations
     */
    override fun click(e: MouseEvent) {
        getCurrentEditModePart()?.let {
            if (!textManipulationModel.isEditing(it) && e.clickCount == 2) {
                startEditing()
            }
            return
        }

        textManipulationModel.commitEditing()
    }

    /**
     * Pressing ENTER:
     * * Enters the editing mode if the current [host] is not being edited
     * * Exits the editing mode if the current [host] is currently being edited
     *
     * Pressing ESC:
     * * Exits + aborts the editing mode if current [host] is currently being edited
     *
     * Pressing any character key
     * * Enters the editing mode if it is not currently active and starts with replacing the current content with
     *   the provided character.
     */
    override fun initialPress(e: KeyEvent?) {
        when (e?.code) {
            KeyCode.ENTER -> {
                if (this.textManipulationModel.isEditing()) {
                    textManipulationModel.commitEditing()
                } else {
                    startEditing()
                }
                e.consume()
            }
            KeyCode.ESCAPE -> {
                if (this.textManipulationModel.isEditing()) {
                    textManipulationModel.abortEditing()
                }
            }
            else -> {
                handleKeyEvent(e)
            }
        }
    }

    override fun finalRelease(e: KeyEvent?) {

    }

    override fun press(e: KeyEvent?) {
        handleKeyEvent(e)
    }

    override fun abortPress() {}

    override fun release(e: KeyEvent?) {}

    private fun handleKeyEvent(keyEvent: KeyEvent?) {
        if (keyEvent?.code == KeyCode.ESCAPE || keyEvent?.code == KeyCode.ENTER) {
            return
        }

        getCurrentEditModePart()?.let { editModePart ->
            if (!this.textManipulationModel.isEditing()) {
                keyEvent?.text?.let {
                    // don't start editing on modifier or other non-alphanumeric keys
                    if (it.isNotEmpty()) {
                        startEditing()

                        keyEvent.consume()
                    }
                }
            }
        }
    }

    /**
     * Starts the editing process by creating a policy in the [InplaceTextManipulationModel] and initializing this
     * policy.
     */
    private fun startEditing() {
        getCurrentEditModePart()?.let {
            textManipulationModel.startEditing(it, requireDomain())
        }
    }

    /**
     * Gets the current host of the handler as a [TextEditModePart].
     */
    private fun getCurrentEditModePart(): TextEditModePart<*>? {
        if (selectionModel.selectionUnmodifiable.size > 1) {
            return null
        }
        if (selectionModel.selectionUnmodifiable.size == 1) {
            return selectionModel.selectionUnmodifiable.firstOrNull() as? TextEditModePart<*>
        }
        return hoverModel.hover as? TextEditModePart<*>
    }

    /**
     * Returns the [IDomain] in which the current host part is operating, or throws an exception
     * (the domain is required to be able to start editing)
     */
    private fun requireDomain(): IDomain = host.viewer.domain
            ?: throw IllegalStateException("The Part needs to be bound to a Viewer with an IDomain before attempting editing")
}
