package de.uulm.se.couchedit.statecharts.scenarios.include.change

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.statecharts.model.couch.elements.State
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.ParentOf
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.scenarios.BaseStatechartScenario
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator
import de.uulm.se.couchedit.statecharts.testmodel.ChildStateChangeTestDetails
import de.uulm.se.couchedit.systemtestutils.test.SystemTestProcessor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order

/**
 * Test Scenario that checks the performance and the correctness of results of multiple ways to change parent / child
 * relations between States.
 * The initial situation looks like this (1 is the position of the collection of GraphicObjects in the set of
 * constellations):
 * ~~~
 * +--------------------------------------------+
 * | 1                                          |
 * | +------------------+  +------------------+ |
 * | | 1A               |  | 1B               | |
 * | |  +-------------+ |  |                  | |
 * | |  |    1A.A     | |  |                  | |
 * | |  +-------------+ |  |                  | |
 * | +------------------+  +------------------+ |
 * +--------------------------------------------+

 * +--------------------------------------------+
 * | 1_peer                                     |
 * |                                            |
 * |                                            |
 * |                                            |
 * |                                            |
 * |                                            |
 * |                                            |
 * +--------------------------------------------+
 * ~~~
 *
 * The test steps are as follows:
 * 1. Generate [size] instances of the situation displayed above and insert them in one DiffCollection
 * 2. Assert that the state representations were generated correctly and that they are accompanied by correct
 *    [ParentOf] relations.
 *
 * The following steps will all be executed with the first situation to emulate the user making changes in the diagram:
 * 3. Move the representation of state 1A.A into the inner area of the representation of state 1B
 * 4. Assert that State 1A.A is now a child of 1B, but not of 1A (where it has been moved out of) nor 1 (transitive reduction)
 * 5. Move the representation of State 1B to the inner area of the representation of 1_peer (with the same x, width and height)
 *    This is to test whether the [ParentOf] relations are changed correctly if a state representation is moved out of
 *    the middle of the child hierarchy
 * 6. Now assert that:
 *      * 1_peer is the parent of 1B
 *      * 1 is the parent of 1A.A
 *      * 1B no longer is the parent by 1A.A
 * 7. Resize the representation of 1_peer so that it includes the representation of 1.
 *    This is to test whether the [ParentOf] relations are changed correctly if the outer state representation is
 *    moved or resized to include the inner one.
 * 8. Assert that:
 *      * 1_peer is the parent of 1
 *      * 1_peer is (still) the parent of 1B
 *      * Neither 1A nor 1A.A children of 1_peer (transitive reduction)
 * 9. Resize the representation of 1B so that its inner area includes the representation of 1A.A
 *    This is to test what happens if a state representation is in the intersection of two outer state representations.
 * 10. Assert that
 *      * 1A.A is now the child of both 1 and 1A.A
 *      * 1B is still not a child of 1A.A (as their representations only intersect and not include each other)
 * 11. Remove the representation of State 1
 * 12. Assert that:
 *      * State 1 has been removed
 *      * 1_peer is the parent of 1A
 *      * 1_peer still is the parent of 1B
 *      * 1B still is the parent of 1AA
 *      * 1_peer still is **not** the parent of 1AA
 */
@Suppress("FunctionName")
class ChildStateChangeTest(private val size: Int) : BaseStatechartScenario() {
    override val testInstanceInfo: TestInstanceInfo = TestInstanceInfo(
            "CSCT",
            "ChildStateChangeTest with $size situations",
            ChildStateChangeTestDetails(size, size * 5)
    )

    private val stateMargin: Double = 5.0

    private val minStateRepresentationWidth: Double = 100.0

    private val minStateRepresentationHeight: Double = 100.0

    private val labelHeight: Double = 20.0

    private val roundedEdgeSize = 10.0

    override val systemTestProcessor: SystemTestProcessor by disposableLazy {
        SystemTestProcessor(listOf(
                StateChartAbstractSyntaxElement::class.java,
                Represents::class.java
        ))
    }

    private val stateRepresentationGenerator by disposableLazy {
        StateRepresentationGenerator(roundedEdgeSize)
    }

    private val scenarioSituations by disposableLazy {
        (1..size).map { generateHierarchicalStateSituation(it) }
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `insert all hierarchical state situations`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (situation in scenarioSituations) {
            with(situation) {
                storeAll(a, inputDiffCollection)
                storeAll(aa, inputDiffCollection)
                storeAll(b, inputDiffCollection)
                storeAll(outer, inputDiffCollection)
                storeAll(peer, inputDiffCollection)
            }
        }

        pt(
                inputDiffCollection,
                ChildStateChangeTest::`insert all hierarchical state situations`,
                "Insert ${size * 5} State representations"
        )
    }

    @CouchEditSuiteTest
    @Order(2)
    fun `should have generated correct ParentOf relations`() {
        for (situation in scenarioSituations) {
            assertSituationInterpretedCorrectly(situation)
        }
    }

    private lateinit var firstSituation: HierarchicalStateSituationRepresentation

    /**
     * ~~~
     * +--------------------------------------------+
     * | 1                                          |
     * | +------------------+  +------------------+ |
     * | | 1A               |  | 1B               | |
     * | |                  |  |  +-------------+ | |
     * | |                  |  |  |    1A.A     | | |
     * | |                  |  |  +-------------+ | |
     * | +------------------+  +------------------+ |
     * +--------------------------------------------+

     * +--------------------------------------------+
     * | 1_peer                                     |
     * |                                            |
     * |                                            |
     * |                                            |
     * |                                            |
     * |                                            |
     * |                                            |
     * +--------------------------------------------+
     * ~~~
     */
    @CouchEditSuiteTest
    @Order(3)
    fun `move state representation 1AA to neighbor state in same parent`() {
        firstSituation = scenarioSituations.first()

        val stateReprAA = firstSituation.aa
        val stateReprB = firstSituation.b

        val area = stateReprB.stateInteriorArea!!

        val newAA = stateRepresentationGenerator.getStateRepresentationFrom(area, stateReprAA)

        firstSituation.aa = newAA

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(newAA, inputDiffCollection)

        pt(
                inputDiffCollection,
                ChildStateChangeTest::`move state representation 1AA to neighbor state in same parent`,
                "Move state representation 1A.A to 1B representation"
        )
    }

    @CouchEditSuiteTest
    @Order(4)
    fun `should have changed the parent of 1AA to 1B`() {
        val state1AA = getRefToStateRepresentedBy(firstSituation.aa)
        val state1A = getRefToStateRepresentedBy(firstSituation.a)
        val state1B = getRefToStateRepresentedBy(firstSituation.b)
        val state1 = getRefToStateRepresentedBy(firstSituation.outer)

        assertParent(state1AA, state1B)
        assertNotParent(state1AA, state1)
        assertNotParent(state1AA, state1A)
    }

    /**
     * ~~~
     * +--------------------------------------------+
     * | 1                                          |
     * | +------------------+                       |
     * | | 1A               |                       |
     * | |                  |     +-------------+   |
     * | |                  |     |    1A.A     |   |
     * | |                  |     +-------------+   |
     * | +------------------+                       |
     * +--------------------------------------------+

     * +--------------------------------------------+
     * | 1_peer                                     |
     * |                       +------------------+ |
     * |                       | 1B               | |
     * |                       |                  | |
     * |                       |                  | |
     * |                       |                  | |
     * |                       +------------------+ |
     * +--------------------------------------------+
     * ~~~
     */
    @CouchEditSuiteTest
    @Order(5)
    fun `move state representation 1B to 1_peer state`() {
        val stateRepr1B = firstSituation.b
        val stateRepr1Peer = firstSituation.peer

        val outerStateRectangular1B = stateRepr1B.outerStateRectangle.shape as Rectangular

        val newY = stateRepr1Peer.stateInteriorArea!!.y
        val new1BArea = GridAreasGenerator.Area(
                outerStateRectangular1B.x,
                newY,
                outerStateRectangular1B.w,
                outerStateRectangular1B.h
        )

        val newStateRepr1B = stateRepresentationGenerator.getStateRepresentationFrom(new1BArea, stateRepr1B)

        firstSituation.b = newStateRepr1B

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(newStateRepr1B, inputDiffCollection)

        pt(
                inputDiffCollection,
                ChildStateChangeTest::`move state representation 1B to 1_peer state`,
                "Move state representation 1B to 1_peer representation"
        )
    }

    @CouchEditSuiteTest
    @Order(6)
    fun `should have changed the parents of 1AA and 1B correctly`() {
        val state1B = getRefToStateRepresentedBy(firstSituation.b)
        val state1AA = getRefToStateRepresentedBy(firstSituation.aa)
        val state1peer = getRefToStateRepresentedBy(firstSituation.peer)
        val state1 = getRefToStateRepresentedBy(firstSituation.outer)

        assertParent(state1B, state1peer)
        assertParent(state1AA, state1)
        assertNotParent(state1AA, state1B)
    }

    /**
     * ~~~
     * +------------------------------------------------+
     * | 1_peer                                         |
     * | +--------------------------------------------+ |
     * | | 1                                          | |
     * | | +------------------+                       | |
     * | | | 1A               |                       | |
     * | | |                  |     +-------------+   | |
     * | | |                  |     |    1A.A     |   | |
     * | | |                  |     +-------------+   | |
     * | | +------------------+                       | |
     * | +--------------------------------------------+ |
     * |                                                |
     * |                                                |
     * |                         +------------------+   |
     * |                         | 1B               |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         +------------------+   |
     * |                                                |
     * +------------------------------------------------+
     * ~~~
     */
    @CouchEditSuiteTest
    @Order(7)
    fun `resize peer state representation to include representation of state 1`() {
        val outerState1Rectangular = firstSituation.outer.outerStateRectangle.shape as Rectangular

        val stateRepr1peer = firstSituation.peer
        val state1PeerLabelHeight = (stateRepr1peer.labelPosition as StateRepresentationGenerator.LabelPosition.Top).height

        val newPeerStateArea = GridAreasGenerator.Area(
                outerState1Rectangular.x - stateMargin,
                outerState1Rectangular.y - state1PeerLabelHeight,
                outerState1Rectangular.w + 2 * stateMargin,
                outerState1Rectangular.h + 2 * stateMargin + state1PeerLabelHeight + outerState1Rectangular.h
        )

        val newPeerStateRepr = stateRepresentationGenerator.getStateRepresentationFrom(newPeerStateArea, stateRepr1peer)
        firstSituation.peer = newPeerStateRepr

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(newPeerStateRepr, inputDiffCollection)

        pt(
                inputDiffCollection,
                ChildStateChangeTest::`resize peer state representation to include representation of state 1`,
                "Resize peer state representation to include state 1's representation"
        )
    }

    @CouchEditSuiteTest
    @Order(8)
    fun `should have correctly changed the parent of state 1`() {
        val outerState1 = getRefToStateRepresentedBy(firstSituation.outer)
        val peerState1 = getRefToStateRepresentedBy(firstSituation.peer)
        val state1A = getRefToStateRepresentedBy(firstSituation.a)
        val state1AA = getRefToStateRepresentedBy(firstSituation.aa)
        val state1B = getRefToStateRepresentedBy(firstSituation.b)

        assertParent(outerState1, peerState1)
        assertParent(state1B, peerState1)
        assertNotParent(state1A, peerState1)
        assertNotParent(state1AA, peerState1)
    }

    /**
     * ~~~
     * +------------------------------------------------+
     * | 1_peer                                         |
     * | +--------------------------------------------+ |
     * | | 1                                          | |
     * | | +------------------+  +------------------+ | |
     * | | | 1A               |  | 1B               | | |
     * | | |                  |  |  +-------------+ | | |
     * | | |                  |  |  |    1A.A     | | | |
     * | | |                  |  |  +-------------+ | | |
     * | | +------------------+  |                  | | |
     * | +--------------------------------------------+ |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         +------------------+   |
     * |                                                |
     * +------------------------------------------------+
     * ~~~
     */
    @CouchEditSuiteTest
    @Order(9)
    fun `resize state representation 1B to include 1AA again`() {
        val stateRepr1B = firstSituation.b
        val stateRepr1AA = firstSituation.aa

        val stateRepr1BOuterRectangular = stateRepr1B.outerStateRectangle.shape as Rectangular
        val stateRepr1AAOuterRectangular = stateRepr1AA.outerStateRectangle.shape as Rectangular

        val stateRepr1BLabelHeight = (stateRepr1B.labelPosition as StateRepresentationGenerator.LabelPosition.Top).height
        val stateRepr1BBottom = stateRepr1BOuterRectangular.y + stateRepr1BOuterRectangular.h

        val newState1BY = stateRepr1AAOuterRectangular.y - stateRepr1BLabelHeight

        val newStateRepr1BArea = GridAreasGenerator.Area(
                stateRepr1BOuterRectangular.x,
                newState1BY,
                stateRepr1BOuterRectangular.w,
                stateRepr1BBottom - newState1BY
        )

        val newState1BRepr = stateRepresentationGenerator.getStateRepresentationFrom(newStateRepr1BArea, stateRepr1B)
        firstSituation.b = newState1BRepr

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(newState1BRepr, inputDiffCollection)

        pt(
                inputDiffCollection,
                ChildStateChangeTest::`resize state representation 1B to include 1AA again`,
                "Resize representation of state 1B to include representation of state 1AA"
        )
    }

    @CouchEditSuiteTest
    @Order(10)
    fun `should have added 1B to parents of 1AA while keeping 1`() {
        val state1AA = getRefToStateRepresentedBy(firstSituation.aa)
        val state1B = getRefToStateRepresentedBy(firstSituation.b)
        val state1 = getRefToStateRepresentedBy(firstSituation.outer)

        val parentOfRelations = testModelRepository.getRelationsToElement(
                state1AA.id,
                ParentOf::class.java,
                false
        )

        val parents = parentOfRelations.map { (_, rel) -> rel.a }

        assertThat(parents)
                .describedAs("State ${state1AA.id} should have both ${state1B.id} and ${state1.id} as parents")
                .containsExactlyInAnyOrder(state1, state1B)

        assertNotParent(state1B, state1)
    }

    /**
     * ~~~
     * +------------------------------------------------+
     * | 1_peer                                         |
     * |                                                |
     * |                                                |
     * |   +------------------+  +------------------+   |
     * |   | 1A               |  | 1B               |   |
     * |   |                  |  |  +-------------+ |   |
     * |   |                  |  |  |    1A.A     | |   |
     * |   |                  |  |  +-------------+ |   |
     * |   +------------------+  |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         |                  |   |
     * |                         +------------------+   |
     * |                                                |
     * +------------------------------------------------+
     * ~~~
     */
    @CouchEditSuiteTest
    @Order(11)
    fun `remove outer 1 state representation`() {
        val outerState1Representation = firstSituation.outer

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for ((_, element) in outerState1Representation) {
            inputDiffCollection.mergeCollection(testModelRepository.remove(element.id))
        }

        pt(
                inputDiffCollection,
                ChildStateChangeTest::`remove outer 1 state representation`,
                "Remove the representation of the outer 1 state"
        )
    }

    @CouchEditSuiteTest
    @Order(12)
    fun `should have correctly changed parent relations`() {
        val outerState1 = getStateRepresentedBy(firstSituation.outer)

        assertThat(outerState1).isNull()

        val state1A = getRefToStateRepresentedBy(firstSituation.a)
        val state1AA = getRefToStateRepresentedBy(firstSituation.aa)
        val state1B = getRefToStateRepresentedBy(firstSituation.b)
        val state1Peer = getRefToStateRepresentedBy(firstSituation.peer)

        assertParent(state1A, state1Peer)
        assertParent(state1B, state1Peer)
        assertParent(state1AA, state1B)
        assertNotParent(state1AA, state1Peer)
    }


    /**
     * Asserts that the situation as given by [generateHierarchicalStateSituation] has been interpreted correctly.
     */
    private fun assertSituationInterpretedCorrectly(situation: HierarchicalStateSituationRepresentation) {
        with(situation) {
            val aState = getStateRepresentedBy(a)!!.ref()
            val aaState = getStateRepresentedBy(aa)!!.ref()
            val bState = getStateRepresentedBy(b)!!.ref()
            val outerState = getStateRepresentedBy(outer)!!.ref()

            assertParent(aState, outerState)
            assertParent(bState, outerState)
            assertParent(aaState, aState)

            assertNotParent(aaState, outerState)
        }

    }

    /**
     * Generates an initial test situation for this scenario (image see main [ChildStateChangeTest] documentation).
     * This is more specifically generated than for other scenarios as the goal of this scenario
     * is to check specific changes in concrete syntax and the speed with which they are handled.
     */
    private fun generateHierarchicalStateSituation(xPos: Int): HierarchicalStateSituationRepresentation {
        /**
         * in each state representation, we need space for:
         * - Two innermost states (as both 1A and 1B need to accomodate 1A.A in the scenario (2 * [minStateRepresentationWidth])
         * - Margins (8 * [stateMargin]):
         *    - Two margins to either side of the innermost 1A.A states
         *    - Two margins to the left and right of 1A and 1B, respectively
         *    - One margin between A and B
         */
        val outerStateRepresentationWidth = (2 * minStateRepresentationWidth + 7 * stateMargin)

        val outerStateRepresentationHeight = minStateRepresentationHeight + 2 * labelHeight + 4 * stateMargin

        val x = xPos * (outerStateRepresentationWidth + 2 * stateMargin)

        /**
         * Area for xA.A
         */
        val innerMostStateArea1AA = GridAreasGenerator.Area(
                x + 2 * stateMargin, // 1 margin in 1 to 1A, 1 margin in 1A to 1A.A
                2 * (labelHeight + stateMargin), // leave room for the labels of 1A and 1 and two margins
                minStateRepresentationWidth,
                minStateRepresentationHeight
        )

        val innerAreaWidth = minStateRepresentationWidth + 2 * stateMargin
        val innerAreaHeight = minStateRepresentationHeight + labelHeight + 2 * stateMargin

        /**
         * Area for xA
         */
        val innerStateArea1A = GridAreasGenerator.Area(
                x + stateMargin,
                labelHeight + stateMargin,
                innerAreaWidth,
                innerAreaHeight
        )

        /**
         * Area for xB
         */
        val innerStateArea1B = GridAreasGenerator.Area(
                x + minStateRepresentationWidth + 4 * stateMargin, // 1 margin left to 1A, 2 margins to each side of 1AA, 1 margin right to 1A
                labelHeight + stateMargin,
                innerAreaWidth,
                innerAreaHeight
        )

        /**
         * Area for x
         */
        val outerStateArea = GridAreasGenerator.Area(x, 0.0, outerStateRepresentationWidth, outerStateRepresentationHeight)

        /**
         * Area for x_peer
         */
        val peerArea = GridAreasGenerator.Area(x, outerStateRepresentationHeight + stateMargin, outerStateRepresentationWidth, outerStateRepresentationHeight)

        val outerStateRepr = stateRepresentationGenerator.getStateRepresentationFrom(
                outerStateArea,
                xPos.toString(),
                StateRepresentationGenerator.LabelPosition.Top(labelHeight)
        )

        val peerStateRepresentation = stateRepresentationGenerator.getStateRepresentationFrom(
                peerArea,
                "${xPos}_peer",
                StateRepresentationGenerator.LabelPosition.Top(labelHeight)
        )

        val aRepr = stateRepresentationGenerator.getStateRepresentationFrom(
                innerStateArea1A,
                "${xPos}A",
                StateRepresentationGenerator.LabelPosition.Top(labelHeight)
        )

        val bRepr = stateRepresentationGenerator.getStateRepresentationFrom(
                innerStateArea1B,
                "${xPos}B",
                StateRepresentationGenerator.LabelPosition.Top(labelHeight)
        )

        val aaRepr = stateRepresentationGenerator.getStateRepresentationFrom(
                innerMostStateArea1AA,
                "${xPos}AA",
                StateRepresentationGenerator.LabelPosition.Top(labelHeight)
        )

        return HierarchicalStateSituationRepresentation(
                outer = outerStateRepr,
                peer = peerStateRepresentation,
                a = aRepr,
                b = bRepr,
                aa = aaRepr
        )
    }

    private fun getRefToStateRepresentedBy(stateRepr: StateRepresentationGenerator.StateRepresentation): ElementReference<State> {
        return getStateRepresentedBy(stateRepr)!!.ref()
    }

    private class HierarchicalStateSituationRepresentation(
            var outer: StateRepresentationGenerator.StateRepresentation,
            var peer: StateRepresentationGenerator.StateRepresentation,
            var a: StateRepresentationGenerator.StateRepresentation,
            var b: StateRepresentationGenerator.StateRepresentation,
            var aa: StateRepresentationGenerator.StateRepresentation
    )
}
