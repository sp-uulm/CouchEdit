package de.uulm.se.couchedit.processing.common.repository.graph

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryRead
import de.uulm.se.couchedit.processing.common.repository.graph.GraphBasedModelRepository.RelationTargetEdge.Type
import org.jgrapht.graph.AbstractGraph
import org.jgrapht.graph.AsUnmodifiableGraph
import java.lang.Integer.min
import java.util.*
import kotlin.collections.HashSet

/**
 * Base class for [ModelRepositoryRead] implementation based on a JGraphT [AbstractGraph]
 */
abstract class GraphBasedModelRepository : ModelRepositoryRead {
    protected abstract val graph: AbstractGraph<ElementReference<*>, RelationTargetEdge>

    override fun getRelationsFromElement(elementId: String): ElementQueryResult<Relation<*, *>> {
        val ref = this.getElementReference(elementId)

        if (!this.graph.containsVertex(ref)) {
            return ElementQueryResult()
        }

        return this.graph.outgoingEdgesOf(ref).getRelationsByEdges(elementId)
    }

    override fun getRelationsToElement(elementId: String): ElementQueryResult<Relation<*, *>> {
        val ref = this.getElementReference(elementId)

        if (!this.graph.containsVertex(ref)) {
            return ElementQueryResult()
        }

        return this.graph.incomingEdgesOf(ref).getRelationsByEdges(elementId)
    }

    override fun getRelationsAdjacentToElement(elementId: String): ElementQueryResult<Relation<*, *>> {
        val ref = this.getElementReference(elementId)

        if (!this.graph.containsVertex(ref)) {
            return ElementQueryResult()
        }

        return this.graph.edgesOf(ref).getRelationsByEdges(elementId)
    }

    override fun <T : Relation<*, *>> getRelationsFromElement(
            elementId: String,
            relationType: Class<T>,
            subTypes: Boolean
    ): ElementQueryResult<T> {
        val ref = getElementReference(elementId)

        if (!this.graph.containsVertex(ref)) {
            return ElementQueryResult()
        }

        return this.graph.outgoingEdgesOf(ref).getRelationsOfTypeByEdges(relationType, subTypes, elementId)
    }

    override fun <T : Relation<*, *>> getRelationsToElement(
            elementId: String,
            relationType: Class<T>,
            subTypes: Boolean
    ): ElementQueryResult<T> {
        val ref = getElementReference(elementId)

        if (!this.graph.containsVertex(ref)) {
            return ElementQueryResult()
        }

        return this.graph.incomingEdgesOf(ref).getRelationsOfTypeByEdges(relationType, subTypes, elementId)
    }

    override fun <T : Relation<*, *>> getRelationsAdjacentToElement(
            elementId: String,
            relationType: Class<T>,
            subTypes: Boolean
    ): ElementQueryResult<T> {
        val ref = getElementReference(elementId)

        if (!this.graph.containsVertex(ref)) {
            return ElementQueryResult()
        }

        return this.graph.edgesOf(ref).getRelationsOfTypeByEdges(relationType, subTypes, elementId)
    }

    override fun getRelationsBetweenElements(
            fromId: String,
            toId: String
    ): ElementQueryResult<Relation<*, *>> {
        val elementMap = this.getRelationReferencesBetween(fromId, toId).map { it.id to this[it]!! }.toMap()

        return ElementQueryResult(elementMap)
    }

    override fun <T : Relation<*, *>> getRelationsBetweenElements(
            fromId: String,
            toId: String,
            relationType: Class<T>,
            subTypes: Boolean
    ): ElementQueryResult<T> {
        val elementMap = this.getRelationReferencesBetween(fromId, toId).mapNotNull {
            return@mapNotNull if (it.type == relationType || (subTypes && it.referencesType(relationType))) {
                @Suppress("UNCHECKED_CAST") // suppressed as checked by (it.type == type, elementReference.type is the type of T in ElementReference
                it.id to this[it] as T
            } else null
        }.toMap()

        return ElementQueryResult(elementMap)
    }

    private fun getRelationReferencesBetween(fromId: String, toId: String): Set<ElementReference<Relation<*, *>>> {
        val fromRef = getElementReference(fromId)
        val toRef = getElementReference(toId)

        if (fromRef == null || toRef == null) {
            return emptySet()
        }

        val relationInfoMapper = { it: RelationTargetEdge ->
            val info = it.relationInfo
            if (info.id == fromId || info.id == toId) null else info
        }

        /*
         * We need to intersect the relations referenced by the outgoing edges of from with the incoming relations
         * of to (because every relation is represented by at least two edges, one going to the relation node and one
         * coming from it).
         *
         * Not using Kotlin's convenience functions for intersecting sets helps performance.
         */
        val edgesA = this.graph.outgoingEdgesOf(fromRef).let {
            it.mapNotNullTo(HashSet(it.size), relationInfoMapper)
        }

        return this.graph.incomingEdgesOf(toRef).let {
            it.mapNotNullTo(HashSet(min(it.size, edgesA.size))) { bEdge ->
                val ref = relationInfoMapper(bEdge)

                if (ref in edgesA) ref else null
            }
        }
    }

    /**
     * Converts a list of [RelationTargetEdge]s to a list of [Relation]s.
     *
     * The relation with id = [excludeId] is not returned. This is needed as else every relation would have itself as
     * a dependency.
     */
    private fun Collection<RelationTargetEdge>.getRelationsByEdges(excludeId: String?): ElementQueryResult<Relation<*, *>> {
        return ElementQueryResult(this.mapNotNull {
            val info = it.relationInfo

            if (info.id == excludeId) {
                return@mapNotNull null
            }

            this@GraphBasedModelRepository[info]?.let { value -> value.id to value }
        }.toMap())
    }

    /**
     * Converts a list of [RelationTargetEdge]s to a list of [Relation]s.
     *
     * A [Relation] is only included if:
     * * Its class type is the exact given [type] OR
     * * [includeSubTypes] is true and its class type is a subtype of [type].
     *
     * The relation with id = [excludeId] is not returned. This is needed as else every relation would have itself as
     * a dependency.
     */
    private fun <T : Relation<*, *>> Collection<RelationTargetEdge>.getRelationsOfTypeByEdges(
            type: Class<T>,
            includeSubTypes: Boolean,
            excludeId: String?
    ): ElementQueryResult<T> {
        return ElementQueryResult(this.mapNotNull {
            val info = it.relationInfo

            if (info.id == excludeId) {
                return@mapNotNull null
            }

            return@mapNotNull if (info.type == type || (includeSubTypes && info.referencesType(type))) {
                @Suppress("UNCHECKED_CAST") // suppressed as checked by (it.type == type, elementReference.type is the type of T in ElementReference
                this@GraphBasedModelRepository[info]?.let { value -> value.id to value as T }
            } else null
        }.toMap())
    }

    /**
     * Returns the [ElementReference] stored for the given ID, or <code>null</code> if an element with this ID
     * was not contained in the Repository
     */
    internal abstract fun getElementReference(id: String): ElementReference<*>?

    /**
     * Returns the [RelationTargetEdge] objects that are used to represent the relation with the given [id] in the
     * ModelRepository
     */
    internal abstract fun getRelationTargetEdges(id: String): Set<RelationTargetEdge>?

    /**
     *
     */
    internal fun getGraphView(): AsUnmodifiableGraph<ElementReference<*>, RelationTargetEdge> {
        return AsUnmodifiableGraph(this.graph)
    }

    /**
     * In the internal graph model of CouchEdit, Relations are inserted both as edges and vertices, as it is possible
     * to create a Relation to another Relation.
     *
     * The RelationObjectConnector class represents the connection from an Element vertex [fromRef]
     * to a Relation vertex [toRef] or vice versa, depending on the combination of Relation directedness and [Type]
     * of RelationObjectConnector (A or B).
     *
     * This exists to make it possible to calculate graph properties like connectedness.
     */
    class RelationTargetEdge(
            val fromRef: ElementReference<*>,
            val toRef: ElementReference<*>,
            val relationInfo: ElementReference<Relation<*, *>>,
            val type: Type,
            val isReverse: Boolean
    ) {
        enum class Type {
            A, B
        }

        @Suppress("DuplicatedCode") // generated
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RelationTargetEdge

            if (fromRef != other.fromRef) return false
            if (toRef != other.toRef) return false
            if (relationInfo != other.relationInfo) return false
            if (type != other.type) return false
            if (isReverse != other.isReverse) return false

            return true
        }

        override fun hashCode(): Int {
            return Objects.hash(fromRef, toRef, relationInfo, type, isReverse)
        }
    }
}
