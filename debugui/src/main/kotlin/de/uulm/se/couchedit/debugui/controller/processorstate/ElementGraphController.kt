package de.uulm.se.couchedit.debugui.controller.processorstate

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.layout.mxGraphLayout
import com.mxgraph.model.mxCell
import com.mxgraph.util.mxEvent
import com.mxgraph.util.mxEventObject
import com.mxgraph.util.mxEventSource
import com.mxgraph.view.mxGraph
import de.uulm.se.couchedit.debugui.viewmodel.processorstate.DisplayStyle
import de.uulm.se.couchedit.debugui.viewmodel.processorstate.ElementWrapper
import de.uulm.se.couchedit.debugui.viewmodel.processorstate.ModelRepositoryGraphAdapter
import de.uulm.se.couchedit.debugui.viewmodel.processorstate.RelationSideWrapper
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableSet
import tornadofx.*

/**
 * Controller for providing an [mxGraph] based on the content of a [ModelRepository] ([modelRepository])
 */
internal class ElementGraphController : Controller() {
    /**
     * Currently active listener for selection / deselection events in the [graph]
     */
    private var currentSelectionEventListener: mxEventSource.mxIEventListener? = null

    /**
     * Graph representing the elements in the current [modelRepository]
     */
    val graph = SimpleObjectProperty<ModelRepositoryGraphAdapter?>(null)

    val alwaysAutoLayout = SimpleBooleanProperty(false)

    /**
     * If set to true, Relations that only exist between two Elements will also be displayed as a "diamond" (Rhombus).
     *
     * Else, these Relations will be displayed as a simple edge to save space.
     */
    val oneToOneRelationsAsDiamondProperty = SimpleBooleanProperty(false)

    /**
     * Style with which regular (non-relation) Elements will be labeled in the graph.
     */
    val elementLabelStyleProperty = SimpleObjectProperty(DisplayStyle.ID_TYPE_NAME_FULL)

    /**
     * Style with which Relations will be labeled in the graph. This applies to "diamond" Relation nodes as well as
     * Relations
     */
    val relationLabelStyleProperty = SimpleObjectProperty(DisplayStyle.TYPE_NAME_CAMELHUMPS)

    /**
     * Whether the automatic layouting feature provided by [layoutGraph] is currently functional
     */
    val canLayout
        get() = canLayoutInternal.readOnlyProperty

    val canLayoutInternal = ReadOnlyBooleanWrapper(false)

    /**
     * The [modelRepository] of which the elements should be displayed in [graph]
     */
    val modelRepository = SimpleObjectProperty<ModelRepository?>(null)

    /**
     * List of elements selected graphically in the [graph].
     */
    val selectedElements: ObservableSet<ElementReference<*>> = FXCollections.observableSet()

    init {
        this.modelRepository.addListener { _, _, newValue ->
            this.graph.value?.shutdown()

            val graph = newValue?.let {
                ModelRepositoryGraphAdapter(
                        it,
                        this.oneToOneRelationsAsDiamondProperty.value,
                        this.elementLabelStyleProperty.value,
                        this.relationLabelStyleProperty.value
                )
            }

            this.canLayoutInternal.unbind()
            this.canLayoutInternal.value = false

            this.graph.value = graph

            if (graph == null) {
                return@addListener
            }

            this.canLayoutInternal.bind(graph.canLayout)

            graph.layoutProperty.value = this.generateLayoutFor(graph)
            graph.layout()
            graph.alwaysAutoLayout.value = this.alwaysAutoLayout.value
            this.alwaysAutoLayout.bindBidirectional(graph.alwaysAutoLayout)

            this.selectedElements.clear()

            this.currentSelectionEventListener?.let { this.graph.get()?.selectionModel?.removeListener(it) }

            this.currentSelectionEventListener = mxEventSource.mxIEventListener { sender, evt ->
                // This seems to be correct - seems like JGraphX got added and removed reversed!
                val added = getMxCellListFromEventProperty(evt, "removed").map(this::mxCellToElement)
                val removed = getMxCellListFromEventProperty(evt, "added").map(this::mxCellToElement)

                this.selectedElements.removeAll(removed.mapNotNull { it })
                this.selectedElements.addAll(added.mapNotNull { it })
            }

            graph.selectionModel.addListener(mxEvent.CHANGE, this.currentSelectionEventListener)
        }

        this.oneToOneRelationsAsDiamondProperty.addListener { _, _, newValue -> this.graph.value?.oneToOneRelationsAsDiamond = newValue }
        this.elementLabelStyleProperty.addListener { _, _, newValue -> this.graph.value?.elementLabelStyle = newValue }
        this.relationLabelStyleProperty.addListener { _, _, newValue -> this.graph.value?.relationLabelStyle = newValue }
    }

    /**
     * Executes the auto-layouting algorithm on the current [graph].
     */
    fun layoutGraph() {
        this.graph.value?.layout()
    }

    /**
     * Generates a [mxGraphLayout] object for the given [graph].
     */
    private fun generateLayoutFor(graph: mxGraph): mxGraphLayout = mxHierarchicalLayout(graph).apply {
        isMoveParent = true
    }

    /**
     * Extracts the [Element] from an [mxCell] as given by the current [modelRepository].
     */
    private fun mxCellToElement(cell: mxCell): ElementReference<*>? {
        return when (val cellValue = cell.value) {
            is ElementWrapper<*> -> {
                cellValue.ref
            }
            is RelationSideWrapper -> {
                cellValue.relationRef
            }
            else -> {
                null
            }
        }
    }

    companion object {
        private fun getMxCellListFromEventProperty(event: mxEventObject, propertyKey: String): List<mxCell> {
            return (event.getProperty(propertyKey) as? List<*>)?.filterIsInstance(mxCell::class.java)
                    ?: emptyList()
        }
    }
}
