package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.suggestions.BaseSuggestion
import de.uulm.se.couchedit.model.base.suggestions.SuggestionFor
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.processing.common.repository.ModelRepository

@Singleton
class GraphicObjectQueries @Inject constructor(private val modelRepository: ModelRepository) {
    /**
     * Returns a list of action suggestions currently available for the element with the given [id].
     *
     * TODO put query methods into another class? Or general query system?
     */
    fun getSuggestionsFor(id: String): List<BaseSuggestion> {
        if (this.modelRepository[id] == null) {
            return emptyList()
        }

        return this.modelRepository.getRelationsFromElement(id, SuggestionFor::class.java, true).values.mapNotNull {
            modelRepository[it.a]
        }
    }

    fun getAttributeBagsFor(id: String): Set<AttributeBag> {
        if (this.modelRepository[id] == null) {
            return emptySet()
        }

        return this.modelRepository.getRelationsToElement(id, AttributesFor::class.java, true).values.mapNotNull {
            modelRepository[it.a]
        }.toSet()
    }

    /**
     * Gets all [GraphicObject]s which the Line with the given [id] is to be attached to based on the
     * application data model (= there is a [ConnectionEnd]
     */
    fun getConnectionEndsForLine(id: String): ConnectionEndsForLineQueryResult {
        if (this.modelRepository[id] == null) {
            return ConnectionEndsForLineQueryResult(null, null)
        }

        val connectionEndRelations = this.modelRepository.getRelationsFromElement(id, ConnectionEnd::class.java, true)
                .values.filter { it.probability == ProbabilityInfo.Explicit }

        val startRelations = connectionEndRelations.filter { !it.isEndConnection }
        val endRelations = connectionEndRelations.filter { it.isEndConnection }

        if (startRelations.size > 1) {
            System.err.println("Warning: A line should have at most one Explicit Connection End for its start, " +
                    "got ${startRelations.size}.")
        }

        if (endRelations.size > 1) {
            System.err.println("Warning: A line should have at most one Explicit Connection End for its end, " +
                    "got ${endRelations.size}.")
        }

        val startAttached = startRelations.firstOrNull()?.let(this::getAttachedElementFromConnectionEnd)
        val endAttached = endRelations.firstOrNull()?.let(this::getAttachedElementFromConnectionEnd)

        return ConnectionEndsForLineQueryResult(startAttached, endAttached)
    }

    /**
     * Gets all line GraphicObjects that are to be attached to the Element with the given [id].
     */
    fun getAttachedLines(id: String): Set<AttachedConnectionQueryResult> {
        if (this.modelRepository[id] == null) {
            return emptySet()
        }

        val connectionEndRelations = this.modelRepository.getRelationsToElement(
                id,
                ConnectionEnd::class.java,
                true
        )

        return connectionEndRelations.values.mapNotNull { connectionEnd ->
            if (connectionEnd.probability != ProbabilityInfo.Explicit) {
                return@mapNotNull null
            }

            (modelRepository[connectionEnd.a] as? GraphicObject<*>)?.let {
                AttachedConnectionQueryResult(it, connectionEnd.isEndConnection)
            }
        }.toSet()
    }

    /**
     * Gets the Container for the GraphicObject with the given [id] from the data model (i.e. the Element that this
     * Element has a [de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf] Relation to)
     */
    fun getContainerFor(id: String): GraphicObject<*>? {
        if (modelRepository[id] == null) {
            return null
        }

        val componentOfRelations = this.modelRepository.getRelationsFromElement(
                id,
                ComponentOf::class.java,
                true
        )

        if (componentOfRelations.size > 1) {
            System.err.println("Warning: Any GraphicObject should have at most one outgoing ComponentOf relation, " +
                    "got ${componentOfRelations.size} for $id.")
        }

        return componentOfRelations.values.firstOrNull()?.let { modelRepository[it.b] }
    }

    fun getContainedFor(id: String): Set<GraphicObject<*>> {
        if (modelRepository[id] == null) {
            return emptySet()
        }

        val relations = modelRepository.getRelationsToElement(id, ComponentOf::class.java, true)

        return relations.values.mapNotNull { modelRepository[it.a] }.toSet()
    }

    private fun getAttachedElementFromConnectionEnd(connectionEnd: ConnectionEnd<*, *>): GraphicObject<*>? {
        return this.modelRepository[connectionEnd.b] as? GraphicObject<*>
    }

    data class ConnectionEndsForLineQueryResult(
            val startAttachedElement: GraphicObject<*>?,
            val endAttachedElement: GraphicObject<*>?
    )

    data class AttachedConnectionQueryResult(
            val attachedConnection: GraphicObject<*>,
            val isEnd: Boolean
    )
}
