package de.uulm.se.couchedit.processing.spatial

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import org.assertj.core.api.Assertions

object SpatialTestUtils {
    /**
     * Assertion for DiffCollections after moving Elements together (i.e. all Elements were moved in the
     * same difference.
     *
     * This method asserts the following:
     * * All [SpatialRelation] diffs in the [diffCollection] have an Element from the [movedElements] set as either their
     *   [SpatialRelation.a] or [SpatialRelation.b] Element
     * * No [SpatialRelation] diff in the [diffCollection] has an Element from the [movedElements] set as both its
     *   [SpatialRelation.a] and [SpatialRelation.b] Element
     *
     * @param diffCollection The DiffCollection resulting from the SpatialAbstractor being given the diffs moving the
     *                       Elements
     * @param movedElements  [ElementReference]s which were moved in the input DiffCollection
     */
    fun assertDiffCorrectnessAfterOperation(
            diffCollection: DiffCollection,
            movedElements: Collection<ElementReference<GraphicObject<*>>>
    ) {
        for (diff in diffCollection) {
            val affected = diff.affected as? SpatialRelation ?: continue

            // instead of XOR, use two distinct assertions here so that
            Assertions.assertThat(affected.a !in movedElements || affected.b !in movedElements).describedAs(
                    "The SpatialAbstractor is supposed to not produce Spatial Relation diffs for pairs of Elements that"
                            + "were moved together, but ${affected.a} and ${affected.b} were both moved"
            ).isTrue()

            Assertions.assertThat(affected.a in movedElements || affected.b in movedElements).describedAs(
                    "The SpatialAbstractor is supposed to not produce Spatial Relations for Elements that"
                            + "were not moved, but ${affected.a} and ${affected.b} were both non-moved elements"
            ).isTrue()
        }
    }

    /**
     * Asserts that there are no [SpatialRelation]s in the given [DiffCollection] for which either the
     * [SpatialRelation.a] or [SpatialRelation.b] reference is in the given [forbiddenRefs]
     *
     * @param diffCollection The DiffCollection to perform assertions with
     */
    fun assertNoRelationsAdjacentToRefsInDiffCollection(
            diffCollection: DiffCollection,
            forbiddenRefs: Set<ElementReference<PrimitiveGraphicObject<*>>>,
            assertionMessage: String
    ) {
        for (diff in diffCollection) {
            val affected = diff.affected as? SpatialRelation ?: continue

            Assertions.assertThat(affected.a !in forbiddenRefs && affected.b !in forbiddenRefs).describedAs(assertionMessage).isTrue()
        }
    }
}
