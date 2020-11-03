package de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior

import de.uulm.se.couchedit.client.interaction.Tool
import de.uulm.se.couchedit.client.interaction.pattern.InteractionPattern
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*

/**
 * The [ToolModel] is used to store the current "tool" selected in the application.
 */
class ToolModel {
    val toolProperty: ObjectProperty<Tool?> = SimpleObjectProperty(null)
    val tool: Tool? by toolProperty

    val currentInteractionPatternProperty: ObjectProperty<InteractionPattern?> = SimpleObjectProperty(null)

    /**
     * Shortcut variable to check whether the ToolModel currently is in a "select" state, i.e. select operations
     * should be executed
     */
    val selectToolEnabled: Boolean
        get() = tool == null
}
