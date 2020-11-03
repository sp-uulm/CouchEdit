package de.uulm.se.couchedit.processing.common.repository.queries

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.repository.ModelRepository

/**
 * Utility class providing the ability to execute queries about the related Element(s) on the given (injected)
 * [ModelRepository].
 */
@ProcessorScoped
class RelationGraphQueries @Inject constructor(val modelRepository: ModelRepository) {
    /**
     * For a directed relation type [relationType] and an [ElementReference], gets a single Element that the referenced
     * Element is related to by a [relationType] relation.
     *
     * @return
     *  * The element the [ref] element is related to, iff exactly one [relationType] relation exists from [ref]
     *  * Null, if no [relationType] relation exists from [ref]
     * @throws IllegalStateException if more than one [relationType] relation exists from [ref]
     * @throws IllegalArgumentException if the single [relationType] relation from [ref] is not directed
     */
    fun <A : Element, B : Element> getElementRelatedFrom(
            ref: ElementReference<A>,
            relationType: Class<out OneToOneRelation<A, B>>,
            includeSubTypes: Boolean
    ): B? {
        val relations = this.modelRepository.getRelationsFromElement(ref.id, relationType, includeSubTypes)

        return this.getSingleReferencedElement(relations, OneToOneRelation<*, B>::b)
    }

    /**
     * For a directed relation type [relationType] and an [ElementReference], gets a single Element that is related to the
     * referenced element by a [relationType] relation.
     *
     * @return
     *  * The element related to the [ref] element, iff exactly one [relationType] relation exists towards [ref]
     *  * Null, if no [relationType] relation exists towards [ref]
     * @throws IllegalStateException if more than one [relationType] relation exists towards [ref]
     * @throws IllegalArgumentException if the single [relationType] relation towards [ref] is not directed
     */
    fun <A : Element, B : Element> getElementRelatedTo(
            ref: ElementReference<B>,
            relationType: Class<out OneToOneRelation<A, B>>,
            includeSubTypes: Boolean
    ): A? {
        val relations = this.modelRepository.getRelationsToElement(ref.id, relationType, includeSubTypes)

        return this.getSingleReferencedElement(relations, OneToOneRelation<A, *>::a)
    }

    /**
     * For a directed [relationType] and an ElementReference [ref], returns all Elements that are related with the [ref]
     * Element via a [relationType] relation.
     *
     * @throws IllegalArgumentException If at least one [relationType] relation from [ref] is not directed.
     */
    fun <A : Element, B : Element> getElementsRelatedFrom(
            ref: ElementReference<A>,
            relationType: Class<out Relation<A, B>>,
            includeSubTypes: Boolean): Set<B> {
        val relations = this.modelRepository.getRelationsFromElement(ref.id, relationType, includeSubTypes)

        return getReferencedElements(relations) {
            if (!it.isDirected) {
                throw IllegalArgumentException("Relation must be directed!")
            }

            return@getReferencedElements it.bSet
        }
    }

    /**
     * For a directed [relationType] and an ElementReference [ref], returns all Elements that the [ref] Element is related with
     * via a [relationType] relation.
     *
     * @throws IllegalArgumentException If at least one [relationType] relation to [ref] is not directed.
     */
    fun <A : Element, B : Element> getElementsRelatedTo(
            ref: ElementReference<B>,
            relationType: Class<out Relation<A, B>>,
            includeSubTypes: Boolean): Set<A> {
        val relations = this.modelRepository.getRelationsToElement(ref.id, relationType, includeSubTypes)

        return getReferencedElements(relations) {
            if (!it.isDirected) {
                throw IllegalArgumentException("Relation must be directed!")
            }

            return@getReferencedElements it.aSet
        }
    }

    /**
     * Gets the set of Elements that are referenced by a set of [Relation]s, where the referenced elements are retrieved
     * by the [referenceTransformer] function.
     */
    private fun <R : Relation<*, *>, E : Element> getReferencedElements(
            relations: Map<String, R>,
            referenceTransformer: (R) -> Set<ElementReference<E>>
    ): Set<E> {
        // stores the retrieved references so we don't unnecessarily query
        // multiple times.
        val processedReferences = mutableSetOf<ElementReference<E>>()
        val ret = mutableSetOf<E>()

        for ((_, rel) in relations) {
            require(rel.isDirected) { "Relation must be directed!" }

            val relReferences = referenceTransformer(rel)

            for (ref in relReferences) {
                if (ref in processedReferences) {
                    continue
                }

                modelRepository[ref]?.let { ret.add(it) }
                processedReferences.add(ref)
            }
        }

        return ret
    }

    /**
     * Helper function to get the referenced Element in the situation that none or exactly one [OneToOneRelation] can exist.
     *
     * @throws IllegalStateException if the given [relations] set has more than one element.
     * @throws IllegalArgumentException if the single element of the [relations] set is not directed
     */
    private fun <R : OneToOneRelation<*, *>, E : Element> getSingleReferencedElement(
            relations: Map<String, R>,
            referenceTransformer: (R) -> ElementReference<E>
    ): E? {
        val relCount = relations.size

        if (relCount == 0) {
            return null
        }

        if (relCount == 1) {
            val relation = relations.values.first()

            require(relation.isDirected) { "Relation must be directed!" }

            return modelRepository[referenceTransformer(relation)]!!
        }

        throw IllegalStateException("Expected 0 or 1 relations, got $relCount")
    }
}
