package de.uulm.se.couchedit.statecharts.scenarios

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.statecharts.model.couch.elements.OrthogonalRegion
import de.uulm.se.couchedit.statecharts.model.couch.elements.State
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.ParentOf
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsOrthogonalState
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator.StateRepresentation
import de.uulm.se.couchedit.statecharts.testdata.StateTransitionGenerator
import de.uulm.se.couchedit.systemtestutils.test.BaseSystemTest
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat

/**
 * Collection of utilities useful for system tests operating in the Statechart model
 */
abstract class BaseStatechartScenario : BaseSystemTest() {
    /**
     * Stores all Elements of the [elements] Map's values to the [testModelRepository] and merges the store
     * results to the [diffCollection].
     */
    protected fun storeAll(elements: Map<*, Element>, diffCollection: MutableTimedDiffCollection) {
        for (element in elements.values) {
            diffCollection.mergeCollection(testModelRepository.store(element))
        }
    }

    /**
     * Queries the [testModelRepository] for the [State] that is represented by the Elements in the given [representation]
     * object and returns that State Element.
     */
    protected fun getStateRepresentedBy(representation: StateRepresentation): State? {
        val baseRect = representation.outerStateRectangle

        return getStateElementRepresentedBy(baseRect.id) as? State
    }

    protected fun getOrthogonalStateRepresentedBy(comHSD: ElementReference<CompartmentHotSpotDefinition>): OrthogonalRegion? {
        return getStateElementRepresentedBy(comHSD.id) as? OrthogonalRegion
    }

    protected fun getTransitionRepresentedBy(representation: StateTransitionGenerator.TransitionRepresentation): Transition? {
        return getStateElementRepresentedBy(representation.line.id) as? Transition
    }

    private fun getStateElementRepresentedBy(elementId: String): StateChartAbstractSyntaxElement? {
        val representsRelations = testModelRepository.getRelationsFromElement(
                elementId,
                Represents::class.java,
                true
        )

        val relation = representsRelations.values.firstOrNull()

        return testModelRepository[relation?.b]
    }

    /**
     * Assert that the Element referenced by [childRef] has exactly one [ParentOf] relation, which has the [parentRef]
     * as its [ParentOf.a] property.
     */
    protected fun assertParent(childRef: ElementReference<StateElement>, parentRef: ElementReference<StateChartAbstractSyntaxElement>) {
        val relations = testModelRepository.getRelationsToElement(
                childRef.id,
                ParentOf::class.java,
                true
        )

        assertThat(relations).describedAs("Expected $childRef to have exactly one ParentOf relation").hasSize(1)

        assertThat(relations.values.first().a).describedAs("Expected $parentRef to have a ParentOf relation to $childRef")
                .isEqualTo(parentRef)
    }

    protected fun assertNotParent(childRef: ElementReference<StateElement>, parentRef: ElementReference<StateChartAbstractSyntaxElement>) {
        val relations = testModelRepository.getRelationsBetweenElements(
                parentRef.id,
                childRef.id,
                ParentOf::class.java,
                true
        )

        assertThat(relations).describedAs("$parentRef should not have a ParentOf Relation towards $childRef").isEmpty()
    }

    protected fun assertExactChildren(
            childrenRefs: Collection<ElementReference<StateElement>>,
            parentRef: ElementReference<StateChartAbstractSyntaxElement>
    ) {
        val children = testModelRepository.getRelationsFromElement(parentRef.id, ParentOf::class.java, true)
                .values.map { it.b }

        assertThat(children).containsExactlyInAnyOrderElementsOf(childrenRefs)
    }

    /**
     * Asserts that
     * * The given [StateRepresentation] represents a [State] Element in the abstract syntax
     * * The given [regionRepresentation] is a [CompartmentHotSpotDefinition] which represents an [OrthogonalRegion]
     *   in the abstract syntax
     * * Between these two represented Elements, there is a [ParentOf] relation from the [OrthogonalRegion] to the
     *   [State].
     */
    protected fun assertRepresentedStateParentOfRepresentedRegion(
            stateRepresentation: StateRepresentation,
            regionRepresentation: ElementReference<CompartmentHotSpotDefinition>
    ) {
        val state = getStateRepresentedBy(stateRepresentation)!!
        val region = getOrthogonalStateRepresentedBy(regionRepresentation)!!

        assertParent(state.ref(), region.ref())
    }

    /**
     * Asserts that the [CompartmentHotSpotDefinition] with the given [compartmentId] has a [RepresentsOrthogonalState]
     * Relation attached to it in the [testModelRepository] and returns the "b" side of that Relation.
     */
    protected fun assertCompartmentRepresentsOrthogonalState(compartmentId: String): ElementReference<OrthogonalRegion> {
        val representsRelations = testModelRepository.getRelationsFromElement(
                compartmentId,
                RepresentsOrthogonalState::class.java,
                true
        )

        assertThat(representsRelations).describedAs(
                "Every Compartment that is not further split up should be marked as representing a state, " +
                        "but $compartmentId is not"
        ).hasSize(1)

        return representsRelations.values.first().b
    }

    /**
     * Assert that the [CompartmentHotSpotDefinition] with the given [compartmentId] does **not** have an outgoing
     * [RepresentsOrthogonalState] relation.
     */
    protected fun assertCompartmentDoesNotRepresentOrthogonalState(compartmentId: String) {
        val representsRelations = testModelRepository.getRelationsFromElement(
                compartmentId,
                RepresentsOrthogonalState::class.java,
                true
        )

        assertThat(representsRelations).describedAs(
                "Every Compartment that is further split up should be not marked as representing a state, " +
                        "but $compartmentId is not"
        ).isEmpty()
    }

    /**
     * Assert that no Abstract Syntax [Transition] Relation exists between [sourceStateRef] and [targetStateRef]
     */
    protected fun assertNoTransitionBetween(
            sourceStateRef: ElementReference<StateElement>,
            targetStateRef: ElementReference<StateElement>
    ) {
        val transitions = testModelRepository.getRelationsBetweenElements(
                sourceStateRef.id,
                targetStateRef.id,
                Transition::class.java,
                true
        )

        assertThat(transitions).describedAs(
                "There should not be a transition between ${sourceStateRef.id} and ${targetStateRef.id}"
        ).isEmpty()
    }

}
