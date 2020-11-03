package de.uulm.se.couchedit.statecharts.scenarios.completegraph

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.RepresentsTransition
import de.uulm.se.couchedit.statecharts.scenarios.BaseStatechartScenario
import de.uulm.se.couchedit.statecharts.testdata.StateGridGenerator
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator
import de.uulm.se.couchedit.statecharts.testdata.StateTransitionGenerator
import de.uulm.se.couchedit.statecharts.testmodel.CompleteTransitionGraphTestDetails
import de.uulm.se.couchedit.systemtestutils.test.SystemTestProcessor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order

/**
 * Test case checking the behavior of the system for a grid of states in which every state is connected with every other
 * state by a transition concrete syntax representation.
 *
 * The test will create a grid of [gridSizeX] x [gridSizeY] "simple" (i.e. non-composite) states as given by the
 * [StateGridGenerator] and create a transition connection between each pair of State representations with the
 * [StateTransitionGenerator].
 * The state representations will be [objectDistance] apart, while each transition end will be [connectionEndDistance]
 * away from its corresponding state
 *
 * The test steps are as follows:
 * 1. Generate the aforementioned grid and insert it to the system as one DiffCollection
 * 2. Assert that for each pair of states, at least one abstract syntax [Transition] relation has been created, and
 *    amongst these [Transition] relations there is at least one represented by the designated transition representation
 *    line (depending on the [objectDistance] and [connectionEndDistance] values, other Transitions may inadvertently
 *    also be created but that is the important one)
 * 3. Select one transition representation in the middle of the grid and modify its start point so that the distance
 *    between "origin" state representation and "target" state representation is half that of the previous value
 * 4. Assert that Step 3 has increased the transition's Probability value
 * 5. To the bottom right of the State Representation grid, insert one more state representation with one transition
 *    going to it from the bottom right Element in the grid
 * 6. Assert that this transition representation has also been correctly translated into an abstract syntax [Transition]
 *    Element.
 */
class CompleteTransitionGraphTest(val gridSizeX: Int, val gridSizeY: Int) : BaseStatechartScenario() {
    private val stateHeight = 100.0
    private val stateWidth = 100.0
    private val objectDistance = 100.0
    private val connectionEndDistance = 10.0

    private val roundedEdgeSize = 10.0

    private val gridAreasGenerator by disposableLazy { GridAreasGenerator(stateHeight, stateWidth, objectDistance) }

    private val stateRepresentationGenerator by disposableLazy { StateRepresentationGenerator(roundedEdgeSize) }

    private val stateGridGenerator by disposableLazy { StateGridGenerator(gridAreasGenerator, stateRepresentationGenerator) }
    private val stateTransitionGenerator by disposableLazy { StateTransitionGenerator(connectionEndDistance, listOf(-1)) }

    override val systemTestProcessor by disposableLazy {
        SystemTestProcessor(listOf(
                GraphicObject::class.java,
                StateElement::class.java,
                Represents::class.java,
                Transition::class.java
        ))
    }

    override val testInstanceInfo by lazy {
        val numberOfStates = gridSizeX * gridSizeY

        TestInstanceInfo(
                "CCGT",
                "Complete connection set of grid $gridSizeX x $gridSizeY",
                CompleteTransitionGraphTestDetails(
                        gridSizeX,
                        gridSizeY,
                        numberOfStates,
                        numberOfStates * (numberOfStates - 1)
                )
        )
    }

    private val stateRepresentationGrid by disposableLazy {
        stateGridGenerator.generateGridOfLabeledStates(gridSizeX, gridSizeY)
    }

    /**
     * Table mapping the [de.uulm.se.couchedit.statecharts.testdata.StateGridGenerator.StateRepresentation.stateId]
     * (source = row, target = column) to the representation of the transition created between these two states.
     */
    private val transitionRepresentations by disposableLazy {
        val ret: Table<String, String, StateTransitionGenerator.TransitionRepresentation> = HashBasedTable.create()

        for (cell in stateRepresentationGrid.cellSet()) {
            val stateRepresentation = cell.value!!

            for (otherCell in stateRepresentationGrid.cellSet()) {
                val otherStateRepresentation = otherCell.value!!
                if (otherStateRepresentation.stateId == stateRepresentation.stateId) {
                    continue
                }

                val transitionRepresentation = stateTransitionGenerator.createConnectionBetween(
                        stateRepresentation,
                        otherStateRepresentation
                )

                ret.put(stateRepresentation.stateId, otherStateRepresentation.stateId, transitionRepresentation)
            }
        }

        return@disposableLazy ret
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `insert state and transition representations`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in stateRepresentationGrid.cellSet()) {
            storeAll(cell.value!!, inputDiffCollection)
        }

        for (cell in transitionRepresentations.cellSet()) {
            storeAll(cell.value!!, inputDiffCollection)
        }

        pt(
                inputDiffCollection,
                CompleteTransitionGraphTest::`insert state and transition representations`,
                "Insert ${stateRepresentationGrid.size()} state representations and ${transitionRepresentations.size()} transition " +
                        "representations"
        )
    }

    @CouchEditSuiteTest
    @Order(2)
    fun `should have generated a Transition Element between each two states`() {
        for (cell1 in stateRepresentationGrid.cellSet()) {
            val state1 = getStateRepresentedBy(cell1.value!!)!!

            for (cell2 in stateRepresentationGrid.cellSet()) {
                val state2 = getStateRepresentedBy(cell2.value!!)!!

                if (state1 == state2) {
                    continue
                }

                val transitions = testModelRepository.getRelationsBetweenElements(
                        state1.id,
                        state2.id,
                        Transition::class.java,
                        true
                )

                assertThat(transitions)
                        .describedAs("There must be a transition between $state1 and $state2").isNotEmpty()

                val transitionRepresentation = transitionRepresentations.get(cell1.value!!.stateId, cell2.value!!.stateId)!!

                val representedByLine = testModelRepository.getRelationsFromElement(
                        transitionRepresentation.line.id,
                        RepresentsTransition::class.java,
                        false
                )

                assertThat(representedByLine).describedAs("Line ${transitionRepresentation.line} is expected to be " +
                        "marked as representing a transition").isNotEmpty()

                val representedTransitions = representedByLine.values.map { testModelRepository[it.b] }

                val representedTransitionsInStateTransitions = transitions.values.filter { it in representedTransitions }

                assertThat(representedTransitionsInStateTransitions)
                        .describedAs("At least one of the transitions detected for line " +
                                "${transitionRepresentation.line.id} must be registered as a transition between " +
                                "${state1.id} and ${state2.id}")
            }
        }
    }

    lateinit var representedTransitionBeforeMove: Transition

    @CouchEditSuiteTest
    @Order(3)
    fun `move one transition further towards its endpoint Element`() {
        val stateRepresentation1X = if (gridSizeX == 1) 0 else (gridSizeX / 2) - 1
        val stateRepresentation1Y = if (gridSizeY == 1) 0 else (gridSizeY / 2) - 1
        val stateRepresentation2X = if (gridSizeX == 1) 0 else stateRepresentation1X + 1
        val stateRepresentation2Y = if (gridSizeY == 1) 0 else stateRepresentation1Y + 1

        val stateRepresentation1 = stateRepresentationGrid.get(stateRepresentation1Y, stateRepresentation1X)
        val stateRepresentation2 = stateRepresentationGrid.get(stateRepresentation2Y, stateRepresentation2X)

        val transitionRepresentation = transitionRepresentations.get(
                stateRepresentation1.stateId,
                stateRepresentation2.stateId
        )

        val representedState1 = getStateRepresentedBy(stateRepresentation1)!!
        val representedState2 = getStateRepresentedBy(stateRepresentation2)!!

        val representsRelations = testModelRepository.getRelationsFromElement(
                transitionRepresentation.line.id,
                RepresentsTransition::class.java,
                true
        )

        representedTransitionBeforeMove = representsRelations.values.mapNotNull { relation ->
            val representedTransition = testModelRepository[relation.b]!!

            return@mapNotNull if (representedTransition.a.id == representedState1.id
                    && representedTransition.b.id == representedState2.id) {
                representedTransition
            } else null
        }.first()

        // now perform move closer to the "from" shape (cut the distance in half)
        val lineShape = transitionRepresentation.line.shape as Line

        val dx = lineShape.start.x - transitionRepresentation.fromShapeBorderPoint.x
        val dy = lineShape.start.y - transitionRepresentation.fromShapeBorderPoint.y

        val newStart = Point(lineShape.start.x - 0.5 * dx, lineShape.start.y - 0.5 * dy)

        lineShape.start = newStart

        val inputDiffCollection = testModelRepository.store(transitionRepresentation.line)

        pt(
                inputDiffCollection,
                CompleteTransitionGraphTest::`move one transition further towards its endpoint Element`,
                "Move transition further towards its 'from' element representation"
        )
    }

    @Order(4)
    @CouchEditSuiteTest
    fun `move operation should have increased the transition probability`() {
        val representedTransitionAfterMove = testModelRepository[representedTransitionBeforeMove.id]!!

        val probabilityBefore = (representedTransitionBeforeMove.probability as ProbabilityInfo.Generated).probability
        val probabilityAfter = (representedTransitionAfterMove.probability as ProbabilityInfo.Generated).probability

        assertThat(probabilityAfter)
                .describedAs(
                        "The transition should have a higher " +
                                "probability after its representation has been moved closer to its endpoint State " +
                                "than before"
                ).isGreaterThan(probabilityBefore)
    }

    lateinit var newStateRepresentation: StateRepresentationGenerator.StateRepresentation
    lateinit var newTransitionRepresentation: StateTransitionGenerator.TransitionRepresentation
    lateinit var bottomRightStateRepresentation: StateRepresentationGenerator.StateRepresentation

    @Order(5)
    @CouchEditSuiteTest
    fun `insert a new State with one attached transition to the bottom right of the Grid`() {
        bottomRightStateRepresentation = stateRepresentationGrid.get(gridSizeY - 1, gridSizeX - 1)

        val outerRoundedRect = bottomRightStateRepresentation.outerStateRectangle.shape as Rectangular

        val (x, y) = with(outerRoundedRect) {
            x + w + objectDistance to y + h + objectDistance
        }

        newStateRepresentation = stateRepresentationGenerator.getStateRepresentationFrom(GridAreasGenerator.Area(
                x,
                y,
                stateWidth,
                stateHeight
        ), "_newlyInserted")

        newTransitionRepresentation = stateTransitionGenerator.createConnectionBetween(
                bottomRightStateRepresentation,
                newStateRepresentation
        )

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(newStateRepresentation, inputDiffCollection)
        storeAll(newTransitionRepresentation, inputDiffCollection)

        pt(
                inputDiffCollection,
                CompleteTransitionGraphTest::`insert a new State with one attached transition to the bottom right of the Grid`,
                "Inserting one new state to grid"
        )
    }

    @Order(6)
    @CouchEditSuiteTest
    fun `should have inserted new abstract syntax State and Transition Elements`() {
        val bottomRightGridState = getStateRepresentedBy(bottomRightStateRepresentation)!!
        val newState = getStateRepresentedBy(newStateRepresentation)!!

        val transitionsBetween = testModelRepository.getRelationsBetweenElements(
                bottomRightGridState.id,
                newState.id,
                Transition::class.java,
                true
        )

        assertThat(transitionsBetween).describedAs("There must be exactly one transition between the states " +
                "${bottomRightGridState.id} and ${newState.id}").hasSize(1)

        val transition = transitionsBetween.values.first()

        val representsTransitionRelations = testModelRepository.getRelationsToElement(
                transition.id,
                RepresentsTransition::class.java,
                true
        )

        assertThat(representsTransitionRelations).describedAs("There must be exactly one RepresentsTransition relation " +
                "towards Transition ${transition.id}").hasSize(1)

        val representsTransition = representsTransitionRelations.values.first()

        val lineGraphicObject = newTransitionRepresentation.line

        assertThat(representsTransition.a.id).describedAs("Transition ${transition.id} " +
                "must be represented by Line ${lineGraphicObject.id}").isEqualTo(lineGraphicObject.id)
    }
}
