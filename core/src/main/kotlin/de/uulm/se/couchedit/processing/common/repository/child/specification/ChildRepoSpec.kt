package de.uulm.se.couchedit.processing.common.repository.child.specification

import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.relations.Relation

class ChildRepoSpec(
        val filters: Set<Filter>
)

class ChildRepoSpecBuilder {
    private val filters = mutableListOf<Filter>()

    fun classes(init: FilterBuilder.ElementType.() -> Unit) {
        val builder = FilterBuilder.ElementType()

        builder.init()

        filters.add(builder.build())
    }

    fun relationsWithEndpoints(
            applyToRelationType: Class<out Relation<*, *>>,
            init: FilterBuilder.RelationsWithEndPoints.() -> Unit
    ) {
        val builder = FilterBuilder.RelationsWithEndPoints(applyToRelationType)

        builder.init()

        filters.add(builder.build())
    }

    fun transitiveReduction(regarding: Class<out OneToOneRelation<*, *>>) {
        filters.add(Filter.TransitiveReduction(regarding))
    }

    fun build(): ChildRepoSpec {
        return ChildRepoSpec(filters.toSet())
    }
}

fun childRepoSpec(init: ChildRepoSpecBuilder.() -> Unit): ChildRepoSpec {
    val builder = ChildRepoSpecBuilder()

    builder.init()

    return builder.build()
}
