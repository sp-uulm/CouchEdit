package de.uulm.se.couchedit.processing.common.repository.graph


import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.RelationCache
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.common.services.diff.VersionManager
import de.uulm.se.couchedit.util.extensions.ref
import org.jgrapht.graph.*
import java.util.*

/**
 * Implementation of [ModelRepository] with the help of a JGraphT [Pseudograph].
 */
@ProcessorScoped
class RootGraphBasedModelRepository @Inject constructor(
        private val diffCollectionFactory: DiffCollectionFactory,
        private val relationCache: RelationCache,
        private val versionManager: VersionManager
) : GraphBasedModelRepository(), ModelRepository {
    override val graph: AbstractGraph<ElementReference<*>, RelationTargetEdge>
        get() = this.relationGraph

    /**
     * "Row key" is the class of the element stored in this row.
     * The "column key" is the ID of the element.
     * As [OneToOneRelation]s are also Elements, they are stored in this Table, too.
     */
    private val elements: Table<Class<out Element>, String, Element> = HashBasedTable.create()

    /**
     * Map of Element IDs to references for faster access
     */
    private val elementReferences = mutableMapOf<String, ElementReference<*>>()

    /**
     * Map from relation IDs to the
     * [de.uulm.se.couchedit.processing.common.repository.graph.GraphBasedModelRepository.RelationTargetEdge]s
     * representing the relations in the [relationGraph].
     */
    private val relationTargetEdges = mutableMapOf<String, MutableSet<RelationTargetEdge>>()

    /**
     * Graph storing the [Element]s of this Repository (by their ID which can be resolved by using the [elements] Table)
     */
    private val relationGraph = DirectedPseudograph<ElementReference<*>, RelationTargetEdge>(RelationTargetEdge::class.java)

    private val listenableGraph = DefaultListenableGraph(relationGraph)

    /**
     * For use by child repositories
     */
    private val onChangeListeners = mutableMapOf<String, (ModelDiff) -> Unit>()

    override operator fun get(id: String): Element? {
        val foundById = this.elements.column(id).values.toTypedArray()

        check(foundById.size <= 1) {
            String.format("More than one element (of different classes) with ID %s was found!", id)
        }

        return if (foundById.isEmpty()) null else foundById[0].copy()
    }

    @Suppress("UNCHECKED_CAST") // suppressed as checked by ref.type vs. class of ret
    override operator fun <T : Element> get(ref: ElementReference<T>?): T? {
        if (ref == null) {
            return null
        }

        var ret = this.elements.get(ref.type, ref.id)

        if (ret == null) {
            // If the element, is not registered with the exactly same type, try to get it via one of its subtypes.
            ret = this[ref.id]
        }

        if (ret == null) {
            return null
        }

        // Integrity Check
        require(ref.type.isInstance(ret)) {
            "Reference for ${ref.id} was typed ${ref.type}, got ${ret::class.java}"
        }

        return ret.copy() as T
    }

    @Suppress("UNCHECKED_CAST") // The table is filled by the store() function so that no wrong types can appear
    override fun <T : Element> getAll(elementType: Class<out T>): ElementQueryResult<T> {
        synchronized(this) {
            return ElementQueryResult(this.elements.row(elementType).mapValues { (_, element) -> element.copy() as T })
        }
    }

    override fun <T : Element> getAllIncludingSubTypes(elementType: Class<out T>): ElementQueryResult<T> {
        val ret = HashMap<String, T>()

        synchronized(this) {
            for (clazz in this.elements.rowKeySet()) {
                if (elementType.isAssignableFrom(clazz)) {
                    ret.putAll(this.elements.row(clazz).mapValues { (_, element) ->
                        // checked by if(elementType.isAssignableFrom(clazz)) above
                        @Suppress("UNCHECKED_CAST")
                        element.copy() as T
                    })
                }
            }
        }

        return ElementQueryResult(ret)
    }

    override fun getVersion(id: String): VectorTimestamp {
        return versionManager.versionOf(id) ?: relationCache.versionOf(id) ?: VectorTimestamp()
    }

    override fun getElementAndRelated(ref: ElementReference<*>): Map<ElementReference<*>, Element> {
        return this.doGetElementAndRelated(ref, emptySet())
    }

    override fun getElementReference(id: String): ElementReference<*>? {
        return elementReferences[id]
    }

    override fun getRelationTargetEdges(id: String): Set<RelationTargetEdge>? {
        return this.relationTargetEdges[id]?.toSet()
    }

    private fun doGetElementAndRelated(ref: ElementReference<*>, visited: Set<ElementReference<*>>): Map<ElementReference<*>, Element> {
        val element = this[ref]?.copy() ?: return emptyMap()

        val ret = mutableMapOf(ref to element)

        if (element is Relation<*, *>) {
            val newVisited = visited.toMutableSet()
            newVisited.add(ref)

            val allRelatedRefs = element.aSet.toMutableSet()
            allRelatedRefs.addAll(element.bSet)

            allRelatedRefs.filterNot(newVisited::contains)

            allRelatedRefs.forEach {
                ret.putAll(doGetElementAndRelated(it, newVisited))
            }
        }

        return ret
    }

    override fun dump(): TimedDiffCollection {
        synchronized(this) {
            val elements = this.getAllIncludingSubTypes(Element::class.java)

            val ret = diffCollectionFactory.createMutableTimedDiffCollection()

            for ((id, element) in elements) {
                ret.putDiff(ElementAddDiff(element), versionManager.versionOf(id)!!)
            }

            return ret
        }
    }

    override fun store(e: Element, timestamp: VectorTimestamp?): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val copyE = e.copy()

        synchronized(this) {
            if (copyE is Relation<*, *>) {
                this.storeRelation(copyE, ret, timestamp)
            } else {
                this.storeGenericElement(copyE, ret, timestamp)
            }
        }

        return ret
    }

    override fun remove(id: String, timestamp: VectorTimestamp?): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        synchronized(this) {
            this.remove(id, ret, timestamp)
        }

        return ret
    }

    override fun refresh(id: String): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        refresh(id, ret)

        return ret
    }

    private fun refresh(id: String, diffs: MutableTimedDiffCollection) {
        val ref = this.elementReferences[id] ?: return

        diffs.putDiff(ElementAddDiff(this[ref]!!), getVersion(id))

        val relationEdges = this.listenableGraph.edgesOf(ref)

        for (edge in relationEdges) {
            if (edge.relationInfo.id == id) {
                continue
            }

            refresh(id, diffs)
        }
    }

    override fun clear(): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        synchronized(this) {
            val ids = this.elementReferences.keys.toSet()

            for (id in ids) {
                if (this.elementReferences[id] == null) {
                    continue
                }

                ret.mergeCollection(this.remove(id))
            }
        }

        return ret
    }

    /**
     * Removes the [Element] with the given [id] and stores the operations executed in the given [diffs] Collection.
     */
    private fun remove(id: String, diffs: MutableTimedDiffCollection, timestamp: VectorTimestamp?, timestampInDiff: Boolean = true) {
        val toRemoveRef = this.elementReferences[id] ?: return // TODO log this?
        val element = this[toRemoveRef]
                ?: return // if the element did not exist here in the first place, don't need to remove it

        // With the vertex, all edges adjacent to it must also be removed.
        val edgesToRemove = this.listenableGraph.edgesOf(toRemoveRef).toList()

        val removedRelationIds = mutableSetOf<String>()
        for (edge in edgesToRemove) {
            val relationId = edge.relationInfo.id

            if (relationId == id || relationId in removedRelationIds) {
                /*
                 * Edge belongs to the same Relation as we are currently trying to delete
                 * To avoid endless recursion, skip that edge (the edges are automatically deleted below when the
                 * node of the relation itself is deleted.)
                 *
                 * Or: we have already deleted the Element that belongs to this Edge. Nothing to do here.
                 */
                continue
            }

            // Don't pass the given here as the deletion of the relation should not update the relation's timestam
            remove(relationId, diffs, getVersion(relationId), false)
            removedRelationIds.add(relationId)
        }

        this.listenableGraph.removeVertex(toRemoveRef)
        this.elements.remove(toRemoveRef.type, toRemoveRef.id)
        this.elementReferences.remove(toRemoveRef.id)
        this.versionManager.onRemove(id)

        if (element is Relation<*, *>) {
            relationTargetEdges.remove(toRemoveRef.id)
        }

        this.relationCache.onElementRemove(toRemoveRef)

        val diff = ElementRemoveDiff(element)

        diffs.putDiff(diff, if (timestampInDiff) updateAndReturnTimestamp(toRemoveRef.id, timestamp) else VectorTimestamp())

        this.notifyChangeListeners(diff)
    }

    /**
     * Stores any [Element] [e] in this Repository, and inserts the executed operations into [diffs].
     * * If the Element ID is not yet known, it is inserted to the internal data structures, and an [ElementAddDiff] will be added.
     * * If an [Element] with the same ID as [e] is already contained in this [ModelRepository], it is compared via its
     *   [Element.equivalent] method.
     *      * If it is equivalent, nothing is done.
     *      * If it is [Element.contentEquivalent] but the probability does not match and the new probability is explicit,
     *        the [setExplicit] procedure is executed.
     *      * If it is not [Element.contentEquivalent], its new state will be inserted to Repository and an [ElementModifyDiff] will be added.
     *
     * The Element will always be stored with the given [timestamp] or, if null is given, with the current timestamp of the ModelRepository.
     */
    private fun storeGenericElement(e: Element, diffs: MutableTimedDiffCollection, timestamp: VectorTimestamp?) {
        var eRef = elementReferences[e.id]
        val oldE = this[eRef]

        // If the Element is newly inserted, stores the set of Relations which became available after the insertion of
        // the new Element
        var availableRelations: Set<Pair<Relation<*, *>, VectorTimestamp?>>? = null

        if (!this.listenableGraph.containsVertex(eRef)) {
            check(oldE === null) { "The element with ID ${e.id} was contained in the element map, but not in the graph!" }
            check(eRef === null) { "The element with ID ${e.id} was contained in the element references, but not in the graph!" }

            eRef = ElementReference(e.id, e::class.java)

            this.elementReferences[e.id] = eRef
            this.listenableGraph.addVertex(eRef)

            availableRelations = this.relationCache.onElementInsert(eRef)
        }

        val diff = if (oldE != null) {
            require(oldE::class.java == e::class.java) {
                "Previous Element with ID ${e.id} was of type ${oldE::class.java.simpleName}, " +
                        "cannot replace it with new Element of type ${e::class.java.simpleName}"
            }

            checkNotNull(eRef) { "Previous element with id ${e.id} found, but no Element reference stored." }

            if (oldE.equivalent(e)) {
                return
            }

            // TODO: generalize?
            if (oldE.probability == ProbabilityInfo.Explicit) {
                e.probability = ProbabilityInfo.Explicit
            }

            ElementModifyDiff(oldE, e)
        } else {
            ElementAddDiff(e)
        }

        diffs.putDiff(diff, updateAndReturnTimestamp(e.id, timestamp))

        this.elements.put(e.javaClass, e.id, e)

        availableRelations?.forEach { (cachedRelation, cachedTimestamp) ->
            // Store all relations we had saved for later
            this.storeRelation(cachedRelation, diffs, cachedTimestamp)
        }

        this.notifyChangeListeners(diff)
    }

    /**
     * Stores a [OneToOneRelation] in the [ModelRepository].
     * * The [OneToOneRelation] is inserted as a Graph edge (or two edges in the case of an undirected OneToOneRelation)
     *   into the internal representation.
     * * The [OneToOneRelation] is stored to the Repository as any other [Element] would, via the [storeGenericElement] method.
     */
    private fun storeRelation(r: Relation<*, *>, diffs: MutableTimedDiffCollection, timestamp: VectorTimestamp?) {
        val oldR = this[r.id]

        if (oldR != null) {
            oldR as Relation<*, *>

            // The directedness of a relation may never be changed.
            // for directed relations, the aSet and bSet of the old and new Relation must be exactly the same.
            // for undirected relations, they may be swapped.

            val isDirectedChanged = oldR.isDirected != r.isDirected
            val endpointsChanged = (oldR.aSet != r.aSet) || (oldR.bSet != r.bSet)
            val endpointsSwappedIdempotently = endpointsChanged && !oldR.isDirected && ((oldR.aSet == r.bSet) || (oldR.bSet == r.aSet))

            require(!(isDirectedChanged || (endpointsChanged && !endpointsSwappedIdempotently))) {
                "Cannot change relation ${r.id}'s aSet and bSet or directedness after first insertion!"
            }
        } else {
            val missing = r.aSet.filter { this[it] == null }.toMutableSet()
            missing.addAll(r.bSet.filter { this[it] == null })

            // if any Elements are missing, don't insert them here.
            if (missing.isNotEmpty()) {
                this.relationCache.insertRelation(r, timestamp, missing)

                return
            }
        }

        // last, also store the relation itself as an element
        this.storeGenericElement(r, diffs, timestamp)

        for (a in r.aSet) {
            addRelationObjectConnector(a, r, RelationTargetEdge.Type.A)
        }
        for (b in r.bSet) {
            addRelationObjectConnector(b, r, RelationTargetEdge.Type.B)
        }
    }

    /**
     * Updates the timestamp for the given Element [id] and returns its new timestamp.
     *
     * If the [timestamp] is given, the timestamp for the ID will be set to [timestamp].
     * In that case, the caller has to ensure that [timestamp] is parallel or newer than the currently stored
     * version (as in the case of paralellity, there are multiple possible ways to deal with that (overwrite, ignore or in the future merge?)
     *
     * @return **new**, resulting timestamp for Element with the given [id].
     *         If set to null, that means the element with the given [id] was updated locally. The method then registers
     *         a local event, then updates the timestamp for [id] and returns that one.
     *
     * @throws IllegalArgumentException If the given [timestamp] was older than the currently stored
     *                                  time stamp for the element.
     */
    private fun updateAndReturnTimestamp(id: String, timestamp: VectorTimestamp?): VectorTimestamp {
        return if (timestamp !== null) {
            require(versionManager.updateVersion(id, timestamp)) {
                "Current timestamp for $id was newer than given timestamp."
            }
            timestamp
        } else {
            versionManager.registerLocalEvent()
            versionManager.markElementUpdated(id)
        }
    }

    /**
     * Helper which adds the edges between a [relation] and its connected [element].
     * The [type] controls whether the Edge(s) created between [relation] and [element] will represent
     * the A or B side of this Relation.
     * If the relation is non-directional, two Edges will be created (one for each direction), else the direction
     * depends on the [type].
     */
    private fun addRelationObjectConnector(
            element: ElementReference<*>,
            relation: Relation<*, *>,
            type: RelationTargetEdge.Type
    ) {
        val relationEdgeSet = this.relationTargetEdges.getOrPut(relation.id, { mutableSetOf() })

        val relationRef = relation.ref()

        if (type == RelationTargetEdge.Type.A || !relation.isDirected) {
            val connectorToRelation = RelationTargetEdge(
                    element,
                    relationRef,
                    relationRef,
                    type,
                    type == RelationTargetEdge.Type.B
            )

            if (!graph.containsEdge(connectorToRelation)) {
                graph.addEdge(
                        element,
                        relationRef,
                        connectorToRelation
                )

                relationEdgeSet.add(connectorToRelation)
            }
        }

        /*
         * Add Edge (Relation -> AttachedElement) if one of the following is true:
         *
         * * The attached Element is a "B" Element
         * * The relation is undirected
         */
        if (type == RelationTargetEdge.Type.B || !relation.isDirected) {
            val connectorFromRelation = RelationTargetEdge(
                    relationRef,
                    element,
                    relationRef,
                    type,
                    type == RelationTargetEdge.Type.A
            )

            if (!graph.containsEdge(connectorFromRelation)) {
                graph.addEdge(
                        relationRef,
                        element,
                        connectorFromRelation
                )

                relationEdgeSet.add(connectorFromRelation)
            }
        }
    }

    /**
     * Returns a copy of this Repository's graph.
     *
     * The internal data structures of the graph are not copied (shallow clone), but that does not matter as
     * ElementReferences and RelationEdges do not have mutable properties.
     */
    @Suppress("UNCHECKED_CAST")
    fun getClonedGraph(): AbstractBaseGraph<ElementReference<*>, RelationTargetEdge> {
        return this.relationGraph.clone() as AbstractBaseGraph<ElementReference<*>, RelationTargetEdge>
    }

    private fun notifyChangeListeners(modelDiff: ModelDiff) {
        for (listener in this.onChangeListeners.values) {
            listener(modelDiff)
        }
    }

    /**
     * Adds the given [listener] to the set of listeners on this Repository.
     * It is called every time an Element gets added or removed.
     */
    override fun addOnChangeListener(id: String, listener: (ModelDiff) -> Unit) {
        this.onChangeListeners[id] = listener
    }

    override fun removeOnChangeListener(id: String): Boolean {
        return this.onChangeListeners.remove(id) != null
    }
}
