package de.uulm.se.couchedit.processing.common.repository.child.specification

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.relations.Relation

/**
 * Subclasses of this class are the possible criteria by which ModelRepositories can be filtered.
 *
 * They are then gathered in a [ChildRepoSpec] in order to generate a child repository that fulfills their
 */
sealed class Filter {
    enum class Mode {
        EXCLUDE,
        INCLUDEONLY
    }

    abstract class ModalFilter(val mode: Mode): Filter()

    /**
     * Specifies that only [relationType] Relations that have only [aTypes] and [bTypes] Elements as their endpoints
     * (where  [aTypes] and [bTypes] also include their subtypes, and are to be understood as a disjunction)
     * will be included in the Graph.
     * Edges that don't match [relationType]
     */
    class RelationsWithEndpoints<T: Relation<*, *>>(
            mode: Mode,
            val relationType: Class<T>,
            val aTypes: Set<Class<out Element>>? = null,
            val bTypes: Set<Class<out Element>>? = null
    ): ModalFilter(mode)

    /**
     * Specifies that the given element types need to be excluded.
     */
    class ElementType(mode: Mode, val types: Set<Class<out Element>>) : ModalFilter(mode) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ElementType

            if (types != other.types) return false

            return true
        }

        override fun hashCode(): Int {
            return types.hashCode()
        }
    }

    /**
     * Specification for calculating the ModelRepository's subgraph for this OneToOneRelation's transitive reduction,
     * i.e. remove redundant edges of the [OneToOneRelation] type that do nothing for reachability.
     *
     * Before calculating the transitive reduction, the relation nodes of the [regarding] type are replaced with
     * regular One-to-one edges. This means it is not supported to have a [regarding] Relation pointing at other
     * relations.
     *
     * This only works with ModelRepositories filtered to be acyclic!
     */
    data class TransitiveReduction(val regarding: Class<out OneToOneRelation<*, *>>) : Filter()
}

abstract class FilterBuilder {
    abstract fun build(): Filter

    abstract class ModalBuilder: FilterBuilder() {
        protected var mode = Filter.Mode.INCLUDEONLY

        fun setToInclude() {
            mode = Filter.Mode.INCLUDEONLY
        }

        fun setToExclude() {
            mode = Filter.Mode.EXCLUDE
        }
    }

    class ElementType : ModalBuilder() {


        private val types = mutableSetOf<Class<out Element>>()

        operator fun plusAssign(value: Class<out Element>) {
            this.types.add(value)
        }

        operator fun plusAssign(value: Set<Class<out Element>>) {
            this.types.addAll(value)
        }

        override fun build(): Filter {
            return Filter.ElementType(mode, this.types.toSet())
        }
    }

    class RelationsWithEndPoints(private val relationType: Class<out Relation<*, *>>): ModalBuilder() {

        private var aTypes = mutableSetOf<Class<out Element>>()
        private var bTypes = mutableSetOf<Class<out Element>>()

        fun addAType(type: Class<out Element>) {
            aTypes.add(type)
        }

        fun addATypes(types: List<Class<out Element>>) {
            aTypes.addAll(types)
        }

        fun addBType(type: Class<out Element>) {
            bTypes.add(type)
        }

        fun addBTypes(types: List<Class<out Element>>) {
            bTypes.addAll(types)
        }

        override fun build(): Filter {
            return Filter.RelationsWithEndpoints(
                    mode,
                    relationType,
                    if(aTypes.isEmpty()) null else aTypes,
                    if(bTypes.isEmpty()) null else bTypes
            )
        }
    }
}
