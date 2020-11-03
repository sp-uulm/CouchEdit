package de.uulm.se.couchedit.client.controller.canvas.gef.part

import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.BaseVisual
import javafx.beans.property.StringProperty
import javafx.scene.Node
import org.eclipse.gef.common.adapt.IAdaptable
import org.eclipse.gef.mvc.fx.parts.IVisualPart

/**
 * Interface for all [BasePart]s that have the possibility to enter a special "edit mode" on double click
 */
interface TextEditModePart<V> : IAdaptable, IVisualPart<V> where V : Node, V : BaseVisual {
    /**
     * The current, committed text of the part.
     * If this property is changed, the Part must publish the text to the rest of the system.
     */
    var text: String

    /**
     * Property through which the currently edited (non-committed) text can be read / manipulated.
     */
    val stagingTextProperty: StringProperty

    /**
     * Enters editing mode
     */
    fun startEditing()

    /**
     * Leaves editing mode and returns the new text entered by the user.
     */
    fun stopEditing(): String

    /**
     * @return Whether the Part is currently in Edit Mode
     */
    fun isEditing(): Boolean

    /**
     * Deselects the text in this part's visual and positions the cursor at the end.
     */
    fun caretToEnd()
}
