package de.uulm.se.couchedit.debugui.view.processorstate

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.view.mxGraph
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.uulm.se.couchedit.debugui.controller.processorstate.ElementGraphController
import de.uulm.se.couchedit.debugui.util.fx.generateView
import de.uulm.se.couchedit.debugui.viewmodel.processorstate.DisplayStyle
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableSet
import javafx.embed.swing.SwingNode
import javafx.geometry.Orientation
import javafx.scene.control.*
import tornadofx.*
import javax.swing.SwingUtilities

/**
 * Fragment showing a graph of Elements contained in a single [modelRepository]
 */
class ElementGraphFragment : Fragment() {
    private val mxGraphComponent = mxGraphComponent(mxGraph())

    private val swingNode = SwingNode()

    private val controller: ElementGraphController by inject()

    val modelRepository = SimpleObjectProperty<ModelRepository?>().also(controller.modelRepository::bindBidirectional)

    val selectedElements: ObservableSet<ElementReference<*>> = controller.selectedElements

    private val elementLabelStyle = generateElementStyleMenuItems()

    private val relationLabelStyle = generateElementStyleMenuItems()

    override val root = borderpane {
        top = toolbar {
            button("Layout") {
                tooltip = Tooltip("Apply automatic layouting now")
                action { this@ElementGraphFragment.controller.layoutGraph() }
                enableWhen(this@ElementGraphFragment.controller.canLayout)
            }

            togglebutton("Automatic") {
                graphic = generateView(FontAwesomeIcon.COGS)
                tooltip = Tooltip("Always keep the graph automatically layouted.\n" +
                        "Manual moving of nodes will be reverted on every change if this is active.")
                enableWhen(this@ElementGraphFragment.controller.canLayout)
                selectedProperty().bindBidirectional(controller.alwaysAutoLayout)
            }

            separator(Orientation.VERTICAL) { }

            button {
                addClass("icon-only")
                graphic = generateView(FontAwesomeIcon.SEARCH_PLUS)
                tooltip = Tooltip("Zoom +")
                action { mxGraphComponent.zoomIn() }
                disableWhen(controller.graph.isNull)
            }

            button {
                addClass("icon-only")
                graphic = generateView(FontAwesomeIcon.SEARCH_MINUS)
                tooltip = Tooltip("Zoom -")
                action { mxGraphComponent.zoomOut() }
                disableWhen(controller.graph.isNull)
            }

            separator(Orientation.VERTICAL) { }

            togglebutton("") {
                graphic = generateView(FontAwesomeIcon.DIAMOND)
                tooltip = tooltip("Also show 1:1 relations as diamonds")
                selectedProperty().bindBidirectional(controller.oneToOneRelationsAsDiamondProperty)
            }

            menubutton {
                graphic = generateView(FontAwesomeIcon.TAG)
                tooltip = tooltip("Element Labels")
                items.addAll(elementLabelStyle.second.values)
            }

            menubutton {
                graphic = generateView(FontAwesomeIcon.ARROWS_ALT)
                tooltip = tooltip("Transition Labels")
                items.addAll(relationLabelStyle.second.values)
            }
        }
        center = swingNode
    }

    init {
        SwingUtilities.invokeLater {
            this.swingNode.apply {
                content = mxGraphComponent

                /*
                 * Hack. For visualizing the current state of a ModelRepository we use JGraphX.
                 * Unfortunately, this library seems to be long out of development and throws "IndexOutOfBounds"
                 * exceptions constantly (see e.g. this bug report:
                 * https://www.aquaclusters.com/app/home/project/public/aquadatastudio/issue/1285)
                 *
                 * Apart from that, there is a well-known bug in JFX 8 - 9 with Drag and Drop in SwingNodes, which also
                 * causes an exception.
                 * As a workaround, simply silence exceptions happening in the AWT thread.
                 */
                Thread.setDefaultUncaughtExceptionHandler { thread: Thread, throwable: Throwable ->
                    println("Error occurred in JGraphX view: $throwable")
                }
            }
        }

        this.controller.elementLabelStyleProperty.bind(this.elementLabelStyle.first.selectedToggleProperty().objectBinding {
            (it?.userData as? DisplayStyle) ?: DisplayStyle.ID_TYPE_NAME_FULL
        })

        this.elementLabelStyle.first.selectToggle(this.elementLabelStyle.second[controller.elementLabelStyleProperty.value]!!)

        this.controller.relationLabelStyleProperty.bind(this.relationLabelStyle.first.selectedToggleProperty().objectBinding {
                (it?.userData as? DisplayStyle) ?: DisplayStyle.TYPE_NAME_CAMELHUMPS
            })

        this.relationLabelStyle.first.selectToggle(this.relationLabelStyle.second[controller.relationLabelStyleProperty.value]!!)

        this.controller.elementLabelStyleProperty.addListener { _, _, newValue ->
           this.elementLabelStyle.first.selectToggle(elementLabelStyle.second[newValue]!!)
        }

        this.controller.relationLabelStyleProperty.addListener { _, _, newValue ->
            this.relationLabelStyle.first.selectToggle(relationLabelStyle.second[newValue]!!)
        }

        this.controller.graph.addListener { _, _, newValue ->
            this.mxGraphComponent.graph = newValue ?: mxGraph()
            this.mxGraphComponent.repaint()
        }
    }

    companion object {
        /**
         * Returns the String to show in the UI for the given [style]
         */
        private fun getDisplayStyleString(style: DisplayStyle): String {
            // do this with "when" to be sure the selection is exhaustive
            return when(style) {
                DisplayStyle.ID_ONLY -> "ID only"
                DisplayStyle.TYPE_NAME_CAMELHUMPS -> "Type (CamelHumps)"
                DisplayStyle.TYPE_NAME_FULL -> "Type (Class name)"
                DisplayStyle.ID_TYPE_NAME_CAMELHUMPS -> "ID + Type (CamelHumps)"
                DisplayStyle.ID_TYPE_NAME_FULL -> "ID + Type (Class name)"
            }
        }

        private fun generateElementStyleMenuItems(): Pair<ToggleGroup, BiMap<DisplayStyle, RadioMenuItem>> {
            val toggleGroup = ToggleGroup()

            val menuItems = HashBiMap.create<DisplayStyle, RadioMenuItem>()

            for(style in DisplayStyle.values()) {
                val menuItem = RadioMenuItem(getDisplayStyleString(style))

                menuItem.userData = style
                menuItem.toggleGroup = toggleGroup

                menuItems[style] = menuItem
            }

            return Pair(toggleGroup, menuItems)
        }
    }
}
