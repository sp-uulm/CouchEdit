package de.uulm.se.couchedit.processing.common.repository.graph

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryRead
import org.jgrapht.graph.AbstractGraph

internal open class ChildGraphBasedModelRepository(
        private val parent: GraphBasedModelRepository,
        override var graph: AbstractGraph<ElementReference<*>, RelationTargetEdge> = parent.getGraphView()
) : GraphBasedModelRepository(), ModelRepositoryRead {
    override fun getElementReference(id: String): ElementReference<*>? {
        val ref = parent.getElementReference(id)

        if (graph.containsVertex(ref)) {
            if (ref?.referencesType(Relation::class.java) == true) {
                if (graph.containsVertex(ref)) {
                    if (parent.getRelationTargetEdges(ref.id)?.all(this.graph::containsEdge) != true) {
                        return null
                    }
                }
            }

            return ref
        }

        return null
    }

    override fun <T: Element> getAll(elementType: Class<out T>): ElementQueryResult<T> {
        return parent.getAll(elementType).filterKeys { isContained(it) }
    }

    override fun <T: Element> getAllIncludingSubTypes(elementType: Class<out T>): ElementQueryResult<T> {
        return parent.getAllIncludingSubTypes(elementType).filterKeys { isContained(it) }
    }

    override fun getElementAndRelated(ref: ElementReference<*>): Map<ElementReference<*>, Element> {
        if (isContained(ref.id)) {
            return parent.getElementAndRelated(ref)
        }

        return mapOf()
    }

    override fun get(id: String): Element? {
        val ref = this.getElementReference(id)

        return ref?.let { parent[it] }
    }

    override fun <T : Element> get(ref: ElementReference<T>?): T? {
        if(ref == null) {
            return null
        }

        return if (isContained(ref.id)) parent[ref] else null
    }

    override fun getVersion(id: String): VectorTimestamp {
        return if (isContained(id)) parent.getVersion(id) else VectorTimestamp()
    }

    private fun isContained(id: String): Boolean {
        return getElementReference(id) != null
    }

    override fun getRelationTargetEdges(id: String): Set<RelationTargetEdge>? {
        return if (isContained(id)) parent.getRelationTargetEdges(id) else null
    }
}
