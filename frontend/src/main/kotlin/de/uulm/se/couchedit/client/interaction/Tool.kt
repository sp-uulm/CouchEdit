package de.uulm.se.couchedit.client.interaction

import de.uulm.se.couchedit.client.interaction.input.OnClickHandler
import de.uulm.se.couchedit.client.interaction.input.OnDragHandler
import de.uulm.se.couchedit.client.interaction.input.OnHoverHandler
import de.uulm.se.couchedit.client.interaction.input.OnKeyStrokeHandler
import de.uulm.se.couchedit.client.interaction.pattern.InteractionPattern
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyBooleanProperty

/**
 * Represents a "tool" through which the user can interact with the drawing / canvas.
 *
 * These tools are realized as handlers for mouse and key events, so that they will receive input from the user.
 */
interface Tool : OnClickHandler, OnHoverHandler, OnDragHandler, OnKeyStrokeHandler {
    /**
     * Property containing the current [InteractionPattern], i.e. style of user input excepted.
     *
     * This is also used for displaying of feedback to the user.
     */
    val currentInteractionPatternProperty: ObjectProperty<InteractionPattern?>

    /**
     * Property indicating whether this tool is currently "active", i.e. accepting input from the user.
     * How a tool is activated depends on its specific
     */
    val activeProperty: ReadOnlyBooleanProperty

    /**
     * Represents the "activeness" of this tool as a reguar variable.
     */
    val isActive: Boolean

    /**
     * Cancels the current interaction with this tool.
     */
    fun deactivate()
}
