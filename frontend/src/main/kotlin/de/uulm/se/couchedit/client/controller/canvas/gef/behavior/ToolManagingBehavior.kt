package de.uulm.se.couchedit.client.controller.canvas.gef.behavior

import de.uulm.se.couchedit.client.controller.canvas.gef.handler.ElementCreationHandler
import de.uulm.se.couchedit.client.interaction.Tool
import de.uulm.se.couchedit.client.interaction.creationtool.CreationTool
import de.uulm.se.couchedit.client.interaction.creationtool.CreationToolCallbacks
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.InplaceTextManipulationModel
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import de.uulm.se.couchedit.model.base.Element
import javafx.beans.value.ObservableValue
import org.eclipse.gef.mvc.fx.behaviors.AbstractBehavior
import org.eclipse.gef.mvc.fx.models.SelectionModel
import tornadofx.*

/**
 * [AbstractBehavior] reacting to the changes in the [ToolModel], passing the correct callbacks to the tools selected.
 */
class ToolManagingBehavior : AbstractBehavior() {
    private val viewer by lazy {
        host.root.viewer
    }

    private val toolModel by lazy {
        viewer.getAdapter(ToolModel::class.java) ?: throw IllegalStateException("No ToolModel bound to viewer!")
    }

    private val selectionModel by lazy {
        viewer.getAdapter(SelectionModel::class.java)
                ?: throw IllegalStateException("No SelectionModel bound to viewer!")
    }

    private val textEditModel by lazy {
        viewer.getAdapter(InplaceTextManipulationModel::class.java)
                ?: throw IllegalStateException("No InplaceTextManipulationModel bound to viewer!")
    }

    private val elementCreationHandler by lazy {
        host.root.getAdapter(ElementCreationHandler::class.java)
    }

    /**
     * Callback passed to the currently active CreationTool in order for it to be able to pass created Elements into the
     * application data model.
     */
    private val creationListener = object : CreationToolCallbacks {
        override fun finishCreation(toInsert: Set<Element>) {
            elementCreationHandler.createElements(toInsert)
        }

        override fun abortCreation() {}
    }

    override fun doActivate() {
        this.toolModel.toolProperty.addListener(this::onToolChange)

        super.doActivate()
    }

    override fun doDeactivate() {
        this.toolModel.toolProperty.removeListener(this::onToolChange)

        super.doDeactivate()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToolChange(
            observableValue: ObservableValue<out Tool?>,
            oldValue: Tool?,
            newValue: Tool?
    ) {
        // remove dependencies on previously selected tool
        oldValue?.let {
            deactivate()

            (it as? CreationTool)?.callbacks = null
        }

        if (newValue == null) {
            this.toolModel.currentInteractionPatternProperty.unbind()
            this.toolModel.currentInteractionPatternProperty.set(null)
        } else {
            // add information to new tool
            this.toolModel.currentInteractionPatternProperty.cleanBind(newValue.currentInteractionPatternProperty)

            (newValue as? CreationTool)?.callbacks = this.creationListener
        }

        // clear selection and stop all text editing when changing tools
        selectionModel.clearSelection()
        textEditModel.abortEditing()
    }
}
