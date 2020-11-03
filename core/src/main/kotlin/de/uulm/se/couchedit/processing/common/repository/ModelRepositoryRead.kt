package de.uulm.se.couchedit.processing.common.repository

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp

/**
 * Every ModificationPort has its own ModelRepository, which represents its "own little world".
 * No references model objects should be shared between those different Repositories, so that no problems with thread
 * safety will occur.
 * The ModelRepository contains the relevant upstream elements for the ModificationPort at hand as well as the target
 * information (the latter is the information for which the ModificationPort wants to publish Diffs for).
 *
 * This interface only provides read access to the model repository.
 */
interface ModelRepositoryRead {
    /**
     * Finds all Elements of the given type.
     *
     * @param elementType Class of Element that should be found.
     * @return Mapping from Element IDs to Element objects (which are all of the type elementType).
     */
    fun <T : Element> getAll(elementType: Class<out T>): ElementQueryResult<T>

    /**
     * Returns all Elements that are of the given [elementType] or one of its subtypes.
     */
    fun <T : Element> getAllIncludingSubTypes(elementType: Class<out T>): ElementQueryResult<T>

    /**
     * Gets all [Relation]s that originate from the element with the ID [elementId].
     *
     * If [elementId] does not exist in this Repository, an empty list is returned.
     */
    fun getRelationsFromElement(elementId: String): ElementQueryResult<Relation<*, *>>

    /**
     * Gets all [Relation]s that go to the element with the ID [elementId].
     * If [elementId] does not exist in this Repository, an empty list is returned.
     */
    fun getRelationsToElement(elementId: String): ElementQueryResult<Relation<*, *>>

    /**
     * Gets all [Relation]s that go to OR originate from the element with the ID [elementId].
     * If [elementId] does not exist in this Repository, an empty list is returned.
     */
    fun getRelationsAdjacentToElement(elementId: String): ElementQueryResult<Relation<*, *>>

    /**
     * Gets the relations that originate from the given element (or are undirected relations)
     *
     * @param elementId    ID of the element for which Relations should be found.
     * @param relationType The type of relation to query for.
     * @return Set of relations originating from this Element.
     */
    fun <T : Relation<*, *>> getRelationsFromElement(
            elementId: String,
            relationType: Class<T>,
            subTypes: Boolean = false
    ): ElementQueryResult<T>

    /**
     * Gets the relations that go towards the given element (or are undirected relations) and are of a specific type.
     *
     * @param elementId    ID of the element for which Relations should be found.
     * @param relationType The type of relation to query for.
     * @return Mapping from the ID of the relation to the [OneToOneRelation] object.
     */
    fun <T : Relation<*, *>> getRelationsToElement(
            elementId: String,
            relationType: Class<T>,
            subTypes: Boolean = false
    ): ElementQueryResult<T>

    /**
     * Gets the relations that go to OR originate from the given element (or are undirected relations)
     *
     * @param elementId    ID of the element for which Relations should be found.
     * @param relationType The type of relation to query for.
     * @return Mapping from the ID of the relation to the [Relation] object.
     */
    fun <T : Relation<*, *>> getRelationsAdjacentToElement(
            elementId: String,
            relationType: Class<T>,
            subTypes: Boolean = false
    ): ElementQueryResult<T>

    /**
     * Gets the relations that exist between elements with IDs [fromId] and [toId].
     */
    fun getRelationsBetweenElements(fromId: String, toId: String): ElementQueryResult<Relation<*, *>>

    /**
     * Gets the relations that exist between elements with IDs [fromId] and [toId] (in that direction or undirected)
     * with the given [relationType].
     */
    fun <T : Relation<*, *>> getRelationsBetweenElements(
            fromId: String,
            toId: String,
            relationType: Class<T>,
            subTypes: Boolean = false
    ): ElementQueryResult<T>

    /**
     * Starting from the element specified by [ref], returns the current Element and all related as a map from ID
     * to the current state of [Element].
     * That means, returned are:
     * * If [ref] references a non-Relation Element, only the Element referenced by [ref] is returned
     * * If [ref] references a [Relation], all related elements are returned, along with their related elements.
     */
    fun getElementAndRelated(ref: ElementReference<*>): Map<ElementReference<*>, Element>

    /**
     * Gets an element by its ID, irrespective of its class.
     */
    operator fun get(id: String): Element?

    /**
     * Gets an element by its [ElementReference].
     *
     * @throws IllegalArgumentException If an Element with the same ID, but type incompatible to that specified in the
     *                                  [ElementReference] is found in the ModelRepository
     */
    operator fun <T : Element> get(ref: ElementReference<T>?): T?

    /**
     * Gets the current version of the element with the given [id]
     */
    fun getVersion(id: String): VectorTimestamp
}
