package de.uulm.se.couchedit.client.view.attribute

import de.uulm.se.couchedit.client.view.attribute.edit.TableCellEditControl
import de.uulm.se.couchedit.client.viewmodel.attribute.AtomicAttributeContentViewModel
import de.uulm.se.couchedit.client.viewmodel.attribute.AttributeBagContentViewModel
import de.uulm.se.couchedit.client.viewmodel.attribute.AttributeContentViewModel
import de.uulm.se.couchedit.client.viewmodel.attribute.AttributeViewModel
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.TreeTableCell
import tornadofx.*

/**
 * [TreeTableCell] that is able to display the current value of an [AttributeViewModel] as well as, if it contains
 * an [AtomicAttributeContentViewModel] (and editing is implemented for the Attribute Type), edit the value with the
 * appropriate control.
 *
 * The TableCell queries the [TableCellEditControlFactory] for an edit control group when editing is requested, then
 * inserts that EditControl's actual control into its graphic and hides the text.
 * If the [TableCellEditControlFactory] returns null for the [item] in this TableCell, editing is prevented.
 */
class AttributeTableCell : TreeTableCell<AttributeViewModel, SimpleObjectProperty<AttributeContentViewModel?>>() {
    private var editControl: TableCellEditControl<*>? = null

    /**
     * Property storing an observable string that is displayed in the Cell when the editing mode is not active.
     */
    private var currentTextProperty: ObservableValue<String>? = null

    override fun startEdit() {
        if (!isEditable || !treeTableView.isEditable || !tableColumn.isEditable) {
            return
        }

        val previousValue = (this.item?.value as? AtomicAttributeContentViewModel<*>)?.attributeValueProperty?.value

        val editControl = this.getOrGenerateEditControl() ?: return
        editControl.editEndListener = { commit ->
            /*if (!commit) {
                // If the user has aborted editing, reset the content to its previous value
                (this.item?.value as? AtomicAttributeContentViewModel<*>)?.attributeValueProperty?.let {
                    it.value = previousValue
                }
            }*/

            this.cancelEdit()
        }

        super.startEdit()

        this.textProperty().unbind()
        this.text = null
        this.graphic = editControl.control
    }

    override fun cancelEdit() {
        super.cancelEdit()

        this.textProperty().bind(currentTextProperty)
        this.graphic = null
    }

    override fun updateItem(item: SimpleObjectProperty<AttributeContentViewModel?>?, empty: Boolean) {
        this.item?.removeListener(this::onItemValueUpdate)

        val oldValue = this.item?.value

        super.updateItem(item, empty)

        this.item?.addListener(this::onItemValueUpdate)

        onItemValueUpdate(this.item, oldValue, this.item?.value)

    }

    @Suppress("UNUSED_PARAMETER") // Used as callback in updateItem()
    private fun onItemValueUpdate(
            observable: ObservableValue<out AttributeContentViewModel?>?,
            oldValue: AttributeContentViewModel?,
            newValue: AttributeContentViewModel?) {
        currentTextProperty = this.generateTextBinding(newValue)

        // will be updated in the next startEdit()
        this.cancelEdit()
        this.editControl = null
    }

    private fun generateTextBinding(vm: AttributeContentViewModel?): StringBinding {
        return when (vm) {
            is AtomicAttributeContentViewModel<*> -> vm.attributeValueProperty.asString()
            is AttributeBagContentViewModel -> SimpleStringProperty(vm.attributeObservable.size.toString()).stringBinding() { it }
            else -> SimpleStringProperty("").stringBinding { it }
        }
    }

    private fun getOrGenerateEditControl(): TableCellEditControl<*>? {
        if (this.editControl == null) {
            (this.item.value as? AtomicAttributeContentViewModel<*>)?.let {
                this.editControl = TableCellEditControlFactory.getControl(it)
            }
        }

        return editControl
    }
}
