package de.uulm.se.couchedit.client.view.attribute.edit

import de.uulm.se.couchedit.client.viewmodel.attribute.AtomicAttributeContentViewModel
import de.uulm.se.couchedit.model.attribute.types.EnumAttribute
import javafx.scene.control.ComboBox
import tornadofx.*

/**
 * [TableCellEditControl] allowing the editing of attributes (primarily [EnumAttribute]s) by choosing a value out of
 * several in a [ComboBox].
 */
class ComboBoxEditControl<T>(attributeContentViewModel: AtomicAttributeContentViewModel<T>) : TableCellEditControl<T>(attributeContentViewModel) {
    override val control = ComboBox<T>().apply {
        val attribute = attributeContentViewModel.backingAttribute

        this.items = if (attribute is EnumAttribute<T>) {
            attribute.getLegalValues().observable()
        } else {
            listOf(attribute.value).observable()
        }

        maxWidth = java.lang.Double.MAX_VALUE
        prefHeightProperty().bind(parentProperty().doubleBinding {
            return@doubleBinding it?.boundsInLocal?.height ?: 0.0
        })

        this.selectionModel.select(attributeContentViewModel.attributeValueProperty.value)

        this.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            // Exit editing as soon as an option is selected.
            this@ComboBoxEditControl.editEndListener?.invoke(true)
        }
    }

    init {
        control.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            attributeContentViewModel.attributeValueProperty.set(newValue)
        }

        attributeContentViewModel.attributeValueProperty.addListener { _, _, newValue ->
            if (control.selectionModel.selectedItem == null || control.selectionModel.selectedItem != newValue && !control.isShowing) {
                control.selectionModel.select(newValue)
            }
        }
    }
}
