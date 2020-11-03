package de.uulm.se.couchedit.client.view.attribute

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.uulm.se.couchedit.client.controller.attribute.AttributeController
import de.uulm.se.couchedit.client.util.fx.IdEqualsMenuItem
import de.uulm.se.couchedit.client.util.fx.ObservingTreeItem
import de.uulm.se.couchedit.client.util.fx.generateView
import de.uulm.se.couchedit.client.view.SidePanelLayoutParams
import de.uulm.se.couchedit.client.viewmodel.attribute.*
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.control.TreeTableCell
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback
import tornadofx.*

/**
 * [Fragment] allowing the viewing and (TODO) change of Attributes for [GraphicObject]s.
 *
 * The currently viewed [GraphicObject] can be defined by the [editedElement] property.
 *
 * The Fragment will then, via its Controller, independently fetch the AttributeBags currently applying to the
 * [editedElement] and display them in a [TreeTableView].
 * Furthermore, the ID and Type of the [editedElement] will be shown.
 *
 * TODO: The Fragment also allows the user to add new AttributeBags to the Element by a button shown at its bottom.
 * TODO: Editing Attributes is supported by in-place editing facilities.
 */
class AttributeFragment : Fragment() {
    private val controller: AttributeController by inject()

    /**
     * The [GraphicObject] instance of which the attributes are currently updated in the Fragment.
     */
    var editedElement: GraphicObject<*>? by controller.editedElementProperty

    private val elementTypeCaption = label("Type") {
        padding = SidePanelLayoutParams.labelPadding
        font = getBoldFont()
    }

    private val elementIDCaption = label("ID") {
        padding = SidePanelLayoutParams.labelPadding
        font = getBoldFont()
    }

    /**
     * [label] showing the "simple" class name of the element, put top-most in the View
     */
    private val elementTypeLabel = label("") {
        textProperty().bind(controller.elementTypeProperty)
        padding = SidePanelLayoutParams.labelPadding
    }

    /**
     * [label] containing the ID of the selected Element
     */
    private val elementIdLabel = label("") {
        textProperty().bind(controller.elementIdProperty)
        padding = SidePanelLayoutParams.labelPadding
    }

    /**
     * Root Item of the tree to be shown. It always points to the [controller]s AttributeBagValues.
     *
     * Through the [ObservingTreeItem] class, it is ensured that the TreeView is always kept in sync with the actual
     * [AttributeBagContentViewModel]s.
     */
    private val treeRootItem = ObservingTreeItem<AttributeViewModel>(
            RootAttributeViewModel(),
            null,
            {
                if (it is RootAttributeViewModel) {
                    return@ObservingTreeItem this.controller.attributeBagViewModels
                }

                return@ObservingTreeItem when (val content = it.content) {
                    is AttributeBagContentViewModel -> content.attributeObservable
                    is AtomicAttributeContentViewModel<*> -> FXCollections.observableMap(mutableMapOf())
                    else -> throw IllegalArgumentException("Invalid tree content type ${it::class.java}")
                }
            }
    )

    /**
     * [TreeTableView] with columns for [Element] property names, types and values.
     * If an element has properties that are also of the type [Element], a sub-tree is shown.
     */
    private val treeView = TreeTableView(treeRootItem).apply {
        val colName = column("Name", AttributeViewModel::nameProperty)
        colName.prefWidthProperty().bind(this.widthProperty().divide(4).multiply(2))

        val colType = column("Type", AttributeViewModel::simpleTypeNameProperty)
        colType.prefWidthProperty().bind(this.widthProperty().divide(4))

        val colValue = column("Value", AttributeViewModel::contentProperty) {
            cellFactory = Callback<TreeTableColumn<AttributeViewModel, SimpleObjectProperty<AttributeContentViewModel?>>, TreeTableCell<AttributeViewModel, SimpleObjectProperty<AttributeContentViewModel?>>> { AttributeTableCell() }
        }
        colValue.prefWidthProperty().bind(this.widthProperty().divide(4))

        isShowRoot = false

        isEditable = true
    }

    /**
     * [javafx.scene.control.MenuButton] allowing the addition of AttributeBags to the currently edited Element.
     */
    private val addButton = menubutton("Add attributes") {
        graphic = generateView(FontAwesomeIcon.PLUS_SQUARE)

        items.bind(controller.possibleAttributeBags) { bagType ->
            val mi = IdEqualsMenuItem(bagType.simpleName)

            mi.id = "attribute_add_${bagType.name}"
            mi.action { controller.insertAttributeBag(bagType) }

            return@bind mi
        }

        disableProperty().bind(items.sizeProperty.eq(0))
    }

    private val commitButton = button("Save") {
        graphic = generateView(FontAwesomeIcon.SAVE)

        action { controller.commit() }
        enableWhen(controller.isDirtyProperty)
    }

    private val abortButton = button("Abort") {
        graphic = generateView(FontAwesomeIcon.UNDO)

        action { controller.rollback() }
        enableWhen(controller.isDirtyProperty)
    }

    override val root = vbox {
        this += borderpane {
            center {
                vbox {
                    this += hbox {
                        this += elementTypeCaption
                        this += elementTypeLabel
                    }

                    this += hbox {
                        this += elementIDCaption
                        this += elementIdLabel
                    }
                }
            }

            right {
                this += vbox {
                    this += commitButton.also { it.minWidthProperty().bind(this.prefWidthProperty()) }
                    this += abortButton.also { it.minWidthProperty().bind(this.prefWidthProperty()) }
                }
            }
        }

        this += treeView

        treeView.hgrow = Priority.ALWAYS
        treeView.vgrow = Priority.ALWAYS

        this += hbox {
            this += addButton

        }
    }

    companion object {
        /**
         * Helper function to get a bold variant of the system default font.
         */
        private fun getBoldFont(): Font {
            val defaultFont = Font.getDefault()

            return Font.font(defaultFont.family, FontWeight.BOLD, defaultFont.size)
        }
    }
}
