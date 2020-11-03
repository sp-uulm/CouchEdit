package de.uulm.se.couchedit.processing.common.repository.child.graph

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.child.specification.ChildRepoSpec
import de.uulm.se.couchedit.processing.common.repository.graph.ChildGraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository

/**
 * ModelRepository that executes graph filter operations everytime it is accessed and something in the parent repository
 * has changed
 *
 * TODO: Optimize?
 */
internal class GraphCalculatingChildModelRepository(
        private val rootModelRepoParent: RootGraphBasedModelRepository,
        private val childRepoSpec: ChildRepoSpec
) : ChildGraphBasedModelRepository(rootModelRepoParent) {
    private var dirty = true

    init {
        rootModelRepoParent.addOnChangeListener(this.toString(), this::markDirty)
    }

    @Suppress("UNUSED_PARAMETER") // needed for rootModelRepo
    private fun markDirty(diff: ModelDiff) {
        synchronized(this) {
            this.dirty = true
        }
    }

    private fun recalculate() {
        if (!dirty) {
            return
        }

        val graph = rootModelRepoParent.getClonedGraph()

        for(filter in childRepoSpec.filters) {
            FilterHandler.handle(rootModelRepoParent, graph, filter)
        }

        this.graph = graph

            dirty = false
    }

    override fun getElementReference(id: String): ElementReference<*>? {
        synchronized(this) {
            recalculate()

            return super.getElementReference(id)
        }
    }

    override fun <T : Element> getAll(elementType: Class<out T>): ElementQueryResult<T> {
        synchronized(this) {
            recalculate()

            return super.getAll(elementType)
        }
    }

    override fun <T : Element> getAllIncludingSubTypes(elementType: Class<out T>): ElementQueryResult<T> {
        synchronized(this) {
            recalculate()

            return super.getAllIncludingSubTypes(elementType)
        }
    }

    override fun getElementAndRelated(ref: ElementReference<*>): Map<ElementReference<*>, Element> {
        synchronized(this) {
            recalculate()

            return super.getElementAndRelated(ref)
        }
    }

    override fun get(id: String): Element? {
        synchronized(this) {
            recalculate()

            return super.get(id)
        }
    }

    override fun <T : Element> get(ref: ElementReference<T>?): T? {
        if(ref == null) {
            return null
        }

        synchronized(this) {
            recalculate()

            return super.get(ref)
        }
    }

    override fun getVersion(id: String): VectorTimestamp {
        synchronized(this) {
            recalculate()

            return super.getVersion(id)
        }
    }

    override fun getRelationTargetEdges(id: String): Set<RelationTargetEdge>? {
        synchronized(this) {
            recalculate()

            return super.getRelationTargetEdges(id)
        }
    }

    override fun getRelationsFromElement(elementId: String): ElementQueryResult<Relation<*, *>> {
        synchronized(this) {
            recalculate()

            return super.getRelationsFromElement(elementId)
        }
    }

    override fun getRelationsToElement(elementId: String): ElementQueryResult<Relation<*, *>> {
        synchronized(this) {
            recalculate()

            return super.getRelationsToElement(elementId)
        }
    }

    override fun getRelationsAdjacentToElement(elementId: String): ElementQueryResult<Relation<*, *>> {
        synchronized(this) {
            recalculate()

            return super.getRelationsAdjacentToElement(elementId)
        }
    }

    override fun <T : Relation<*, *>> getRelationsFromElement(elementId: String, relationType: Class<T>, subTypes: Boolean): ElementQueryResult<T> {
        synchronized(this) {
            recalculate()

            return super.getRelationsFromElement(elementId, relationType, subTypes)
        }
    }

    override fun <T : Relation<*, *>> getRelationsToElement(elementId: String, relationType: Class<T>, subTypes: Boolean): ElementQueryResult<T> {
        synchronized(this) {
            recalculate()

            return super.getRelationsToElement(elementId, relationType, subTypes)
        }
    }

    override fun <T : Relation<*, *>> getRelationsAdjacentToElement(elementId: String, relationType: Class<T>, subTypes: Boolean): ElementQueryResult<T> {
        synchronized(this) {
            recalculate()

            return super.getRelationsAdjacentToElement(elementId, relationType, subTypes)
        }
    }

    override fun getRelationsBetweenElements(fromId: String, toId: String): ElementQueryResult<Relation<*, *>> {
        synchronized(this) {
            recalculate()

            return super.getRelationsBetweenElements(fromId, toId)
        }
    }

    override fun <T : Relation<*, *>> getRelationsBetweenElements(fromId: String, toId: String, relationType: Class<T>, subTypes: Boolean): ElementQueryResult<T> {
        synchronized(this) {
            recalculate()

            return super.getRelationsBetweenElements(fromId, toId, relationType, subTypes)
        }
    }
}
