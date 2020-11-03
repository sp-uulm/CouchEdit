package de.uulm.se.couchedit.processing.common.repository

import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.util.extensions.ref

/**
 * Class to cache [Relation]s that could not be added to a [ModelRepository] yet because dependent elements could not
 * be yet added.
 */
@ProcessorScoped
class RelationCache {
    /**
     * Map of all relations that are currently stored in this cache as its related elements are missing from the source
     * ModelRepository.
     */
    private val cachedRelations = mutableMapOf<ElementReference<Relation<*, *>>, Relation<*, *>>()

    private val cachedTimeStamps = mutableMapOf<ElementReference<Relation<*, *>>, VectorTimestamp>()

    private val relationReferences = mutableMapOf<String, ElementReference<Relation<*, *>>>()

    /**
     * Map from relation [ElementReference] to elements that are missing for the relation with this [ElementReference]
     */
    private val dependenciesForRelation = mutableMapOf<ElementReference<Relation<*, *>>, MutableSet<ElementReference<*>>>()

    /**
     * Map from Element References to all relations that are dependant on this element, i.e. this element is referenced
     * by the relation but it is missing in the ModelRepository
     */
    private val relationsWaitingFor = mutableMapOf<ElementReference<*>, MutableSet<ElementReference<Relation<*, *>>>>()

    /**
     * Inserts a [Relation] [rel] into the cache with the metadata that [missingElements] need to get inserted before
     * the Element can be handled by the main [ModelRepository].
     *
     * @return True if the relation has been inserted, false if the old state has been preserved (due to the given
     *         [timestamp] being older than the timestamp already in the RelationCache
     *
     * @throws IllegalArgumentException If the [relation] was already inserted in the RelationCache with different
     *                                  [missingElements] values.
     *
     * @throws IllegalArgumentException If the [missingElements] set is empty
     */
    fun insertRelation(rel: Relation<*, *>, timestamp: VectorTimestamp?, missingElements: Set<ElementReference<*>>): Boolean {
        val relRef = rel.ref()

        synchronized(this) {
            this.dependenciesForRelation[relRef]?.let { previouslyMissingElements ->
                require(missingElements.minus(previouslyMissingElements).isEmpty()) {
                    "New missing Elements given contain more Elements than the previous missing Elements of " +
                            "Element ID ${relRef.id}. This is not allowed as onElementRemove() would otherwise have been" +
                            "called and removed all dependent relations from the Cache."
                }
            }


            this.dependenciesForRelation[relRef] = missingElements.toMutableSet()

            for (element in missingElements) {
                this.relationsWaitingFor.getOrPut(element, { mutableSetOf() }).add(relRef)
            }

            timestamp?.let {
                val currentTimestamp = this.cachedTimeStamps[relRef]

                if (currentTimestamp != null
                        && it.relationTo(currentTimestamp) == VectorTimestamp.CausalRelation.STRICTLY_BEFORE) {
                    return false
                }

                this.cachedTimeStamps[relRef] = it
            }
            this.cachedRelations[relRef] = rel
            this.relationReferences[relRef.id] = relRef

            return true
        }
    }

    /**
     * To be called when an Element is added to the main ModelRepository.
     *
     * Returns the set of Relations that are now valid and available as all of their elements were inserted.
     */
    fun onElementInsert(ref: ElementReference<*>): Set<Pair<Relation<*, *>, VectorTimestamp?>> {
        val ret = mutableSetOf<Pair<Relation<*, *>, VectorTimestamp?>>()

        synchronized(this) {
            // get relations that are waiting for [element] to be inserted.
            val relations = relationsWaitingFor.remove(ref) ?: return emptySet()

            for (r in relations) {
                val rMissingElements = dependenciesForRelation[r]
                        ?: throw IllegalStateException("Relations referred in relationsWaitingFor must also be in dependenciesForRelation!")

                rMissingElements.remove(ref)

                if (rMissingElements.isEmpty()) {
                    dependenciesForRelation.remove(r)

                    val relationElement = cachedRelations.remove(r)
                            ?: throw IllegalStateException("Relations referred in dependenciesForRelation must also be in cachedRelations!")

                    ret.add(Pair(relationElement, cachedTimeStamps[r]))
                }
            }

            return ret
        }
    }

    /**
     * To be called whenever an Element [ref] is deleted.
     * This deletes all relations that are dependent ("waiting") on [ref] so we don't get "zombie" relations
     * if it occurs that ref was already inserted.
     */
    fun onElementRemove(ref: ElementReference<*>) {
        synchronized(this) {
            val relationsWaitingOnRef = relationsWaitingFor[ref]?.toSet() ?: emptySet()

            // delete all relations cached for this element.
            for (relationWaitingOnRef in relationsWaitingOnRef) {
                onRelationDelete(relationWaitingOnRef)
            }

            this.relationReferences[ref.id]?.let {
                onRelationDelete(ref.asType())
            }

            relationsWaitingFor.remove(ref)
        }
    }

    fun versionOf(ref: ElementReference<*>): VectorTimestamp? {
        return cachedTimeStamps[ref]
    }

    fun versionOf(id: String): VectorTimestamp? {
        return this.relationReferences[id]?.let { versionOf(it) }
    }

    /**
     * To be called whenever a relation [ref] is deleted. This deletes references to [ref] for all elements on which [ref]
     * was waiting.
     */
    private fun onRelationDelete(ref: ElementReference<Relation<*, *>>) {
        synchronized(this) {
            // get all elements on which the relation which is now being removed is waiting
            val dependenciesOfRef = dependenciesForRelation.remove(ref) ?: return

            for (dependencyOfRef in dependenciesOfRef) {
                // Now remove this relation from the list of missing relations for each element
                val relationsWaitingForDependency = relationsWaitingFor[dependencyOfRef]
                        ?: throw IllegalStateException("Elements in dependenciesForRelation must also have a set of dependent relations in relationsWaitingFor")

                if (!relationsWaitingForDependency.remove(ref)) {
                    throw IllegalStateException("If an element has a dependency entered in dependenciesForRelation, the opposite way must be entered in relationsWaitingFor")
                }

                if (relationsWaitingForDependency.isEmpty()) {
                    relationsWaitingFor.remove(dependencyOfRef)
                }
            }

            this.cachedRelations.remove(ref)
            this.relationReferences.remove(ref.id)
        }
    }
}
