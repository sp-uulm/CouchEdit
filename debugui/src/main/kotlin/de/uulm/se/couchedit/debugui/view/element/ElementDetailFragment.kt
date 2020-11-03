package de.uulm.se.couchedit.debugui.view.element

import de.uulm.se.couchedit.debugui.controller.element.ElementPropertyController
import de.uulm.se.couchedit.debugui.model.elementmeta.ComplexElementFieldValue
import de.uulm.se.couchedit.debugui.model.elementmeta.ElementFieldValue
import de.uulm.se.couchedit.debugui.view.DebugLayoutParams
import de.uulm.se.couchedit.model.base.Element
import javafx.scene.Parent
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * [Fragment] showing information about a given [element].
 *
 * Contains following controls:
 * * [label]s showing the [element]'s type and given ID
 * * [TreeTableView] showing the string representation of [element]'s properties, with a sub-tree showing a sub-[Element]'s
 *   properties if a property contains such a sub-element
 *
 */
class ElementDetailFragment : Fragment() {
    private val controller: ElementPropertyController by inject()

    /**
     * This is not realized via a Property as property listeners don't fire if the old and new Elements
     * are equal (which is not correct in our case because of the contract of [Element.equals]).
     * Instead, we have to check for equivalence!
     */
    var element: Element?
        get() = controller.element
        set(value) {
            controller.element = value
        }

    /**
     * [label] showing the "simple" class name of the element, put top-most in the View
     */
    private val elementTypeLabel = label("") {
        padding = DebugLayoutParams.labelPadding
    }

    /**
     * [label] containing the ID of the selected Element
     */
    private val elementIdLabel = label("") {
        padding = DebugLayoutParams.labelPadding
    }

    /**
     * [TreeTableView] with columns for [Element] property names, types and values.
     * If an element has properties that are also of the type [Element], a sub-tree is shown.
     */
    private val treeView = TreeTableView<ElementFieldValue>(
            TreeItem(ElementFieldValue("root", "", "", ""))
    ).apply {
        val colName = column("Name", ElementFieldValue::nameProperty)
        colName.prefWidthProperty().bind(this.widthProperty().divide(2))

        val colType = column("Type", ElementFieldValue::simpleTypeNameProperty)
        colType.prefWidthProperty().bind(this.widthProperty().divide(4))

        val colValue = column("Value", ElementFieldValue::displayValueProperty)
        colValue.prefWidthProperty().bind(this.widthProperty().divide(4))

        isShowRoot = false
    }

    override val root: Parent = vbox {
        this += elementTypeLabel
        this += elementIdLabel
        this += treeView

        treeView.hgrow = Priority.ALWAYS
        treeView.vgrow = Priority.ALWAYS
    }

    init {
        showNewElement(null, emptyList())

        controller.newDataReadyListener = { newElement, newElementFieldValues ->
            showNewElement(newElement, newElementFieldValues)
        }
    }

    /**
     * Shows the [newElement] with the generated [newElementFieldValues] in this Fragment.
     */
    private fun showNewElement(newElement: Element?, newElementFieldValues: Collection<ElementFieldValue>) {
        runLater {
            val typeText = newElement?.let { it::class.simpleName } ?: NO_ELEMENT_SELECTED
            elementTypeLabel.text = typeText

            val idText = newElement?.let(Element::id) ?: ""
            elementIdLabel.text = idText

            val toolTipText = if (newElement != null)
                "Selected Element\nType: ${newElement::class.qualifiedName}\nID: $idText"
            else NO_ELEMENT_SELECTED

            val toolTip = Tooltip(toolTipText)

            elementTypeLabel.tooltip = toolTip
            elementIdLabel.tooltip = toolTip

            treeView.populate { parent ->
                if (parent == treeView.root) {
                    return@populate newElementFieldValues
                }

                return@populate (parent.value as? ComplexElementFieldValue)?.subFieldsObservable?.values ?: emptyList()
            }
        }
    }

    companion object {
        const val NO_ELEMENT_SELECTED = "No valid element selected"

    }

}
