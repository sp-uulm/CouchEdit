package de.uulm.se.couchedit.client.view.attribute.edit

import de.uulm.se.couchedit.client.viewmodel.attribute.AtomicAttributeContentViewModel
import javafx.scene.Node

abstract class TableCellEditControl<T>(protected val attributeContentViewModel: AtomicAttributeContentViewModel<T>) {
    /**
     * The control allowing to edit the attribute value
     */
    abstract val control: Node

    /**
     * Function that will be called when the control wants to leave the editing mode "on its own" (e.g. pressing ENTER).
     * The Boolean passed to the callback indicates whether the control wants to save or discard the user's current
     * editing value.
     *
     *
     * The committed value is stored in the [attributeContentViewModel] of the EditControl.
     */
    var editEndListener: ((Boolean) -> Unit)? = null
}
