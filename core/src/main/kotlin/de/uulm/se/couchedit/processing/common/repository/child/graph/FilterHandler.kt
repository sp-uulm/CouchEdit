package de.uulm.se.couchedit.processing.common.repository.child.graph

import de.uulm.se.couchedit.jgraphtextensions.TransitiveReduction
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.processing.common.repository.child.specification.Filter
import de.uulm.se.couchedit.processing.common.repository.graph.GraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository
import org.jgrapht.Graph
import org.jgrapht.graph.AbstractBaseGraph

/**
 * Modifies a [Graph] by the conditions given by ChildRepoSpec [Filter]s.
 *
 * TODO: Make this a service; pluggable sub-handlers for different types
 */
object FilterHandler {
    /**
     * Filter the given [graph] according to the criteria of this [filter].
     */
    internal fun handle(
            modelRepository: RootGraphBasedModelRepository,
            graph: AbstractBaseGraph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>,
            filter: Filter
    ) {
        when (filter) {
            is Filter.RelationsWithEndpoints<*> -> {
                handleRelationsWithEndpoints(modelRepository, graph, filter)
            }

            is Filter.ElementType -> {
                handleElementType(modelRepository, graph, filter)
            }

            is Filter.TransitiveReduction -> {
                handleTransitiveReduction(modelRepository, graph, filter)
            }

            // satisfying when-exhaustiveness
            is Filter.ModalFilter -> {
            }
        }
    }

    /**
     * Handles a [Filter.ElementType] filter by removing all non-matching Elements from the [graph] (or all matching,
     * depending on the [Filter.Mode] setting).
     */
    private fun handleElementType(
            modelRepository: RootGraphBasedModelRepository,
            graph: AbstractBaseGraph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>,
            filter: Filter.ElementType
    ) {
        val verticesToRemove = mutableSetOf<ElementReference<*>>()
        val edgesToRemove = mutableSetOf<GraphBasedModelRepository.RelationTargetEdge>()

        for (vertexRef in graph.vertexSet()) {
            val matches = filter.types.any { vertexRef.referencesType(it) }

            /*
             * If the mode is INCLUDEONLY, remove all non-matching elements, i.e. filter them into the toRemove
             * set.
             */
            if ((filter.mode == Filter.Mode.INCLUDEONLY) xor matches) {
                verticesToRemove.add(vertexRef)
            }
        }

        for (relationEdge in graph.edgeSet()) {
            val matches = filter.types.any {
                relationEdge.relationInfo.referencesType(it)
            }

            val removeByFilter = (filter.mode == Filter.Mode.INCLUDEONLY) xor matches

            val removeByEndpointRemoved = graph.getEdgeSource(relationEdge) in verticesToRemove
                    || graph.getEdgeTarget(relationEdge) in verticesToRemove

            if (removeByEndpointRemoved || removeByFilter) {
                edgesToRemove.add(relationEdge)
                verticesToRemove.add(relationEdge.relationInfo)
            }
        }

        graph.removeAllVertices(verticesToRemove)
        graph.removeAllEdges(edgesToRemove)
    }

    /**
     * Handles a [Filter.RelationsWithEndpoints] filter by removing all elements that don't conform to both of the
     * filter's conditions from the graph (or those that conform to both conditions depending on the [Filter.Mode]
     * setting).
     */
    private fun handleRelationsWithEndpoints(
            modelRepository: RootGraphBasedModelRepository,
            graph: AbstractBaseGraph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>,
            filter: Filter.RelationsWithEndpoints<*>
    ) {
        val relationsToRemove = mutableSetOf<ElementReference<*>>()

        edgeLoop@ for (edge in graph.edgeSet()) {
            if (edge.relationInfo in relationsToRemove) {
                continue@edgeLoop
            }

            val givenRelationType = edge.relationInfo.type

            // only consider relations that are a subtype of the filter's defined relation type.
            if (!filter.relationType.isAssignableFrom(givenRelationType)) {
                continue
            }

            var matches = true

            when (edge.type) {
                GraphBasedModelRepository.RelationTargetEdge.Type.A -> {
                    if (filter.aTypes == null) {
                        continue@edgeLoop
                    }

                    /*
                     * Non-reversed A lines go from the A Node to the Relation Node
                     */
                    val aRef = if (edge.isReverse) graph.getEdgeTarget(edge) else graph.getEdgeSource(edge)

                    matches = matches && filter.aTypes.any { aRef.referencesType(it) }
                }
                GraphBasedModelRepository.RelationTargetEdge.Type.B -> {
                    if (filter.bTypes == null) {
                        continue@edgeLoop
                    }

                    /*
                     * Non-reversed B lines go from the Relation Node to the B Node
                     */
                    val bRef = if (edge.isReverse) graph.getEdgeSource(edge) else graph.getEdgeTarget(edge)

                    matches = matches && filter.bTypes.any { bRef.referencesType(it) }
                }
            }

            if ((filter.mode == Filter.Mode.INCLUDEONLY) xor matches) {
                relationsToRemove.add(edge.relationInfo)
            }
        }

        graph.removeAllEdges(graph.edgeSet().filter { it.relationInfo in relationsToRemove })

        // also remove the not included relations as vertices from the graph.
        graph.removeAllVertices(relationsToRemove)
    }

    /**
     * @todo This works for the narrow use case of the
     *       [de.uulm.se.couchedit.processing.containment.controller.ContainmentProcessor], because
     *       [de.uulm.se.couchedit.model.spatial.relations.Include] relations may never depend on another Include
     *       relation.
     *       However, this might need to be generalized for future use cases.
     */
    private fun handleTransitiveReduction(
            modelRepository: RootGraphBasedModelRepository,
            graph: AbstractBaseGraph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>,
            filter: Filter.TransitiveReduction
    ) {
        val isRemovable = fun(edge: GraphBasedModelRepository.RelationTargetEdge): Boolean {
            return edge.relationInfo.referencesType(filter.regarding)
        }

        val mapToEndpoints = fun(
                edge: GraphBasedModelRepository.RelationTargetEdge
        ): Pair<ElementReference<*>, ElementReference<*>>? {
            if (edge.relationInfo.referencesType(filter.regarding)) {
                val relation = modelRepository[edge.relationInfo.asType<OneToOneRelation<*, *>>()]!!

                return Pair(relation.a, relation.b)
            }

            return null
        }

        // exclude self-relations.
        val relevance = fun(edge: GraphBasedModelRepository.RelationTargetEdge): Boolean {
            val relation = modelRepository[edge.relationInfo]!!

            if (edge.toRef in relation.aSet || edge.fromRef in relation.bSet) {
                return false
            }

            return true
        }

        TransitiveReduction.reduce(
                graph,
                mapToEndpoints,
                relevance,
                isRemovable
        )
    }
}
