package de.uulm.se.couchedit.debugui.viewmodel.processorstate

import com.mxgraph.layout.mxGraphLayout
import com.mxgraph.model.mxICell
import com.mxgraph.view.mxGraph
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.RelationCache
import de.uulm.se.couchedit.util.extensions.ref
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty

/**
 * [mxGraph] implementation that listens to changes in a [ModelRepository] and displays them in a JGraphX
 * view for debugging / demonstration purposes.
 *
 * The Graph subscribes via the [ModelRepository.addOnChangeListener] and listens to changes via a
 * RxJava [ReplaySubject] on a separate thread (so that the modelRepository operations are slowed down as little as
 * possible).
 */
class ModelRepositoryGraphAdapter(
        private val modelRepository: ModelRepository,
        oneToOneRelationsAsDiamond: Boolean = false,
        elementLabelStyle: DisplayStyle = DisplayStyle.ID_TYPE_NAME_FULL,
        relationLabelStyle: DisplayStyle = DisplayStyle.TYPE_NAME_CAMELHUMPS
) : mxGraph() {
    /**
     * If set to true, Relations that only exist between two Elements will also be displayed as a "diamond" (Rhombus).
     *
     * Else, these Relations will be displayed as a simple edge to save space.
     */
    var oneToOneRelationsAsDiamond = oneToOneRelationsAsDiamond
        set(value) {
            if (field != value) {
                synchronized(this) {
                    field = value
                    clearAll()
                    rebuild()
                }
            }
        }

    /**
     * Style with which regular (non-relation) Elements will be labeled in the graph.
     */
    var elementLabelStyle = elementLabelStyle
        set(value) {
            synchronized(this) {
                if (field != value) {
                    field = value

                    rebuildContentsOfCells(false)
                }
            }
        }

    /**
     * Style with which Relations will be labeled in the graph. This applies to "diamond" Relation nodes as well as
     * Relations
     */
    var relationLabelStyle = relationLabelStyle
        set(value) {
            synchronized(this) {
                if (field != value) {
                    field = value

                    rebuildContentsOfCells(true)
                }
            }
        }

    /**
     * Layout generator for this graph, providing the [layout] functionality
     */
    val layoutProperty = SimpleObjectProperty<mxGraphLayout?>(null)

    /**
     * Whether the automatic layouting feature provided by [layout] is currently functional
     */
    val canLayout = layoutProperty.isNotNull!!

    /**
     * If set to true, the graph will automatically apply its [layout] after every insert / remove.
     */
    val alwaysAutoLayout = SimpleBooleanProperty(false)

    /**
     * Like the RelationCache in the ModelRepository itself, used to save Relations for which the dependencies do not
     * yet exist.
     */
    private val relationCache = RelationCache()

    /**
     * Mapping from ElementReferences to the "main" cells representing them in the mxGraph.
     * * For regular Elements, this maps to the single cell representing the Element.
     * * For one to one Relations, if [oneToOneRelationsAsDiamond] is false, this maps to the edge representing
     *   the Relation.
     * * For one-to-many Relations or if [oneToOneRelationsAsDiamond] is true, this maps to the "Main" node representing
     *   the Relation.
     */
    private val elementToCellMap = mutableMapOf<ElementReference<*>, mxICell>()

    /**
     * For Relations displayed as "Diamonds", maps the Relation's ElementReference to the mxGraph edge cells representing
     * the aSet and bSet of the relation.
     */
    private val relationToEdgeMap = mutableMapOf<ElementReference<*>, Pair<MutableSet<mxICell>, MutableSet<mxICell>>>()

    /**
     * The Subject on which the model changes will be enqueued. We use a [ReplaySubject] because:
     * * This will cache changes if the Graph is currently busy, without blocking the [modelRepository] operations
     * * Decouples the mxGraph thread from the one manipulating the modelRepository.
     */
    private val modelDiffSubject = ReplaySubject.create<ModelDiff>()

    /**
     * The [Disposable] for the subscription to [modelDiffSubject]
     */
    private var disposable: Disposable?

    /**
     * Whether this Graph has been [shutdown] and will no longer function as a live view.
     */
    private var isDead = false

    init {
        this.setStylesheet(CouchStyleSheet)

        this.resetEdgesOnMove = true
        this.allowLoops = true
        this.disconnectOnMove = false

        this.modelRepository.addOnChangeListener(getListenerKey()) {
            modelDiffSubject.onNext(it)
        }

        rebuild()

        this.alwaysAutoLayout.addListener { _, _, newValue ->
            if(newValue) {
                layout()
            }
        }

        disposable = this.modelDiffSubject.observeOn(Schedulers.newThread()).subscribe(this::onDiff)
    }

    /**
     * Cleans up the adapter to free up resources.
     */
    fun shutdown() {
        isDead = true

        this.modelRepository.removeOnChangeListener(getListenerKey())

        this.modelDiffSubject.onComplete()
        if (this.disposable?.isDisposed == true) {
            this.disposable?.dispose()
        }
    }

    /**
     * Fetches all [Element]s currently in the [modelRepository] and inserts them to the graph.
     */
    fun rebuild() {
        synchronized(this) {
            val oldAlwaysAutoLayout = this.alwaysAutoLayout.value
            this.alwaysAutoLayout.value = false

            val allElements = this.modelRepository.getAllIncludingSubTypes(Element::class.java)

            for ((_, element) in allElements) {
                this.insertElement(element)
            }

            layout()
            this.alwaysAutoLayout.value = oldAlwaysAutoLayout

            repaint()
        }
    }

    /**
     * Removes all vertices and edges from the graph. Call [rebuild] after this to have the graph consistent with
     * the [modelRepository]'s state again.
     */
    fun clearAll() {
        for (ref in this.elementToCellMap.keys.toSet()) {
            this.removeElement(ref)
        }
    }

    fun layout() {
        layoutProperty.value?.execute(this.getDefaultParent())
    }

    /**
     * Callback for when a [diff] comes in on the [modelDiffSubject].
     */
    private fun onDiff(diff: ModelDiff) {
        when (diff) {
            is ElementAddDiff -> {
                this.insertElement(diff.affected)
            }
            is ElementRemoveDiff -> {
                this.removeElement(diff.affected.ref())
            }
        }
    }

    /**
     * Inserts an [element] reference and all [Element]s that can be inserted with it
     */
    private fun insertElement(element: Element) {
        synchronized(this) {
            getModel().beginUpdate()

            try {
                doInsertElement(element)
            } finally {
                getModel().endUpdate()

                if(alwaysAutoLayout.value) {
                    layout()
                }
            }
        }
    }

    /**
     * Inserts the given [element] into the graph. It is mandatory to call beginUpdate on the Model before.
     */
    private fun doInsertElement(element: Element) {
        if (element is Relation<*, *>) {
            val missingElements = mutableSetOf<ElementReference<*>>()

            missingElements.addAll(element.aSet.filter { it !in this.elementToCellMap })
            missingElements.addAll(element.bSet.filter { it !in this.elementToCellMap })

            if (missingElements.isNotEmpty()) {
                relationCache.insertRelation(element, null, missingElements)

                return
            }

            if (element.aSet.size == 1 && element.bSet.size == 1 && !oneToOneRelationsAsDiamond) {
                insertOneToOneRelationAsEdge(element)

                return
            }
        }

        val style = if (element is Relation<*, *>)
            CouchStyleSheet.RELATION_VERTEX_STYLE_KEY
        else
            CouchStyleSheet.ELEMENT_VERTEX_STYLE_KEY

        val cell = insertVertex(
                defaultParent,
                null,
                this.generateElementDisplayObject(element.ref()),
                0.0,
                0.0,
                0.0,
                0.0,
                style
        )

        this.elementToCellMap[element.ref()] = cell as mxICell

        if (element is Relation<*, *>) {
            insertRelationEdges(element)
        }

        val readyElements = this.relationCache.onElementInsert(element.ref())

        for ((readyElement, _) in readyElements) {
            doInsertElement(readyElement)
        }

        updateCellSize(cell)
    }

    /**
     * For a [relation] being displayed as a diamond shape, inserts the edges for the Relation's [Relation.aSet] and
     * [Relation.bSet] into the graph. This requires that the main "diamond" node has been created and inserted
     * to the [elementToCellMap] before.
     */
    private fun insertRelationEdges(relation: Relation<*, *>) {
        // fetch the relation's "normal, middle" cell
        val relationCell = elementToCellMap[relation.ref()]

        val aEdgeSet = mutableSetOf<mxICell>()
        val bEdgeSet = mutableSetOf<mxICell>()

        val style = if (relation.isDirected) CouchStyleSheet.EDGE_STYLE_DIRECTED else CouchStyleSheet.EDGE_STYLE_UNDIRECTED

        var i = 0
        for (ref in relation.aSet) {
            val dto = RelationSideWrapper("a", i, relation.ref())

            val sourceCell = elementToCellMap[ref]

            val edgeCell = insertEdge(defaultParent, null, dto, sourceCell, relationCell, style) as mxICell

            updateCellSize(edgeCell)

            aEdgeSet.add(edgeCell)

            i++
        }

        i = 0
        for (ref in relation.bSet) {
            val dto = RelationSideWrapper("b", i, relation.ref())

            val targetCell = elementToCellMap[ref]

            val edgeCell = insertEdge(defaultParent, null, dto, relationCell, targetCell, style) as mxICell

            updateCellSize(edgeCell)

            bEdgeSet.add(edgeCell)

            i++
        }

        this.relationToEdgeMap[relation.ref()] = Pair(aEdgeSet, bEdgeSet)
    }

    /**
     * Inserts the given [relation] as just an Edge from its single [Relation.aSet] to its single [Relation.bSet] member.
     */
    private fun insertOneToOneRelationAsEdge(relation: Relation<*, *>) {
        val aRef = relation.aSet.first()
        val bRef = relation.bSet.first()

        val aCell = elementToCellMap[aRef]
        val bCell = elementToCellMap[bRef]

        val style = if (relation.isDirected) CouchStyleSheet.EDGE_STYLE_DIRECTED else CouchStyleSheet.EDGE_STYLE_BIDIRECTIONAL

        val edgeCell = insertEdge(
                defaultParent,
                null,
                generateElementDisplayObject(relation.ref()),
                aCell,
                bCell,
                style
        ) as mxICell

        updateCellSize(edgeCell)

        elementToCellMap[relation.ref()] = edgeCell
    }

    /**
     * Removes the given [ref] Element (node and all associated edges) from the graph.
     */
    private fun removeElement(ref: ElementReference<*>) {
        synchronized(this) {
            val edges = relationToEdgeMap.remove(ref)

            edges?.let { (aEdges, bEdges) -> aEdges.union(bEdges) }?.let {
                val cellArray = it.toTypedArray()

                //selectionModel.removeCells(cellArray)
                removeCells(cellArray)
            }

            val mainCell = elementToCellMap.remove(ref)

            val cellArray = mainCell?.let { arrayOf(it) }

            //selectionModel.removeCells(cellArray)
            removeCells(cellArray)

            if(alwaysAutoLayout.value) {
                layout()
            }
        }
    }

    /**
     * Gets the key with which this Adapter will register and unregister itself at the
     * [ModelRepository.addOnChangeListener] method.
     */
    private fun getListenerKey(): String {
        return this::class.java.name
    }

    /**
     * Generates the appropriate [ElementWrapper] for the given [ref], including the applicable currently set label
     * style.
     */
    private fun <T : Element> generateElementDisplayObject(ref: ElementReference<T>): ElementWrapper<T> {
        return ElementWrapper(ref).also {
            it.displayStyle = if (ref.referencesType(Relation::class.java)) relationLabelStyle else elementLabelStyle
        }
    }

    /**
     * Recreates the cell contents from the current [elementToCellMap].
     *
     * @param relations If this is set to true, the function will regenerate relation cells, else it will regenerate
     *                  non-relation Element cells.
     */
    private fun rebuildContentsOfCells(relations: Boolean) {
        for ((ref, cell) in elementToCellMap) {
            if (!relations xor ref.referencesType(Relation::class.java)) {
                model.setValue(cell, generateElementDisplayObject(ref))

                updateCellSize(cell)
            }
        }

        repaint()
    }
}
