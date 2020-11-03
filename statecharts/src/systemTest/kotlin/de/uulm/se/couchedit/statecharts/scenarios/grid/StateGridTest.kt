package de.uulm.se.couchedit.statecharts.scenarios.grid

import com.google.common.collect.Table
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.statecharts.model.couch.elements.State
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsStateElement
import de.uulm.se.couchedit.statecharts.scenarios.BaseStatechartScenario
import de.uulm.se.couchedit.statecharts.testdata.StateGridGenerator
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator
import de.uulm.se.couchedit.statecharts.testmodel.StateGridTestDetails
import de.uulm.se.couchedit.systemtestutils.test.SystemTestProcessor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order

/**
 * Test case checking the behavior of the system for a grid of "simple" States given in concrete syntax.
 *
 * This test performs the following steps:
 * 1. Generate a grid of [gridSizeX] x [gridSizeY] concrete syntax state representations (rounded rectangle + label)
 *    and insert them as one DiffCollection
 * 2. Assert that for every concrete syntax representation, a [State] abstract syntax Element has been created and that
 *    the [State]'s text is equal to the concrete syntax representation's label's content text.
 * 3. Move a set of [numberOfStatesToMove] concrete syntax state representations out of the middle of the grid so that
 *    they now are to the right of the previously right border of the state representation grid.
 * 4. Assert that all moved Elements still represent the same state, as such purely graphical changes should not change
 *    the abstract syntax representation of the statechart.
 * 5. Insert a new grid of [additionalGridSizeX] x [additionalGridSizeY] state representations
 * 6. Assert that for all of the new grid elements of step 5, an abstract syntax State representation has also been
 *    inserted
 * 7. Remove [toRemoveGridSizeX] x [toRemoveGridSizeY] state representations from the top left of the grid
 * 8. Assert that for all of the removed state representations, the corresponding state has also been deleted.
 */
class StateGridTest(
        private val gridSizeX: Int,
        private val gridSizeY: Int,
        private val numberOfStatesToMove: Int,
        private val additionalGridSizeX: Int,
        private val additionalGridSizeY: Int,
        private val toRemoveGridSizeX: Int,
        private val toRemoveGridSizeY: Int
) : BaseStatechartScenario() {
    private val stateHeight = 100.0
    private val stateWidth = 100.0
    private val objectDistance = 20.0
    private val roundedEdgeSize = 10.0

    override val systemTestProcessor by lazy {
        SystemTestProcessor(listOf(
                StateElement::class.java,
                RepresentsStateElement::class.java,
                GraphicObject::class.java
        ))
    }

    private val gridAreasGenerator by disposableLazy { GridAreasGenerator(stateHeight, stateWidth, objectDistance) }

    private val stateGridGenerator by disposableLazy {
        StateGridGenerator(
                gridAreasGenerator,
                StateRepresentationGenerator(roundedEdgeSize)
        )
    }

    override val testInstanceInfo: TestInstanceInfo = TestInstanceInfo(
            "StateGridTest",
            "StateGridTest - $gridSizeX x $gridSizeY, move $numberOfStatesToMove Elements",
            StateGridTestDetails(
                    gridSizeX,
                    gridSizeY,
                    gridSizeX * gridSizeY,
                    numberOfStatesToMove,
                    additionalGridSizeX,
                    additionalGridSizeY,
                    additionalGridSizeX * additionalGridSizeY
            )
    )

    private lateinit var stateRepresentationGrid: Table<Int, Int, StateRepresentationGenerator.StateRepresentation>

    @BeforeAll
    fun generateTestData() {
        this.stateRepresentationGrid = stateGridGenerator.generateGridOfLabeledStates(
                gridSizeX,
                gridSizeY
        )
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `insert state representations`() {
        val inputDiffCollection = insertAllElementsFromGrid(stateRepresentationGrid)

        pt(inputDiffCollection, StateGridTest::`insert state representations`, "Insert $gridSizeX x $gridSizeY state representations")
    }

    private lateinit var representationToState: Map<String, State>

    @CouchEditSuiteTest
    @Order(2)
    fun `should have inserted abstract syntax State objects with correct labels`() {
        representationToState = assertStateRepresentationsInserted(stateRepresentationGrid)
    }

    val movedBaseRectangles = mutableSetOf<String>()

    @CouchEditSuiteTest
    @Order(3)
    fun `move states to the right of the grid`() {
        val movedStateElements = stateGridGenerator.moveStatesToRight(stateRepresentationGrid, numberOfStatesToMove)

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()
        for (elements in movedStateElements) {
            elements.values.forEach {
                inputDiffCollection.mergeCollection(testModelRepository.store(it))
            }

            movedBaseRectangles.add(elements.outerStateRectangle.id)
        }

        pt(
                inputDiffCollection,
                StateGridTest::`move states to the right of the grid`,
                "Move $numberOfStatesToMove state representations to the right"
        )
    }

    @CouchEditSuiteTest
    @Order(4)
    fun `should leave abstract syntax state Elements in place after move`() {
        for (graphicObjectId in movedBaseRectangles) {
            val representsRelations = testModelRepository.getRelationsFromElement(
                    graphicObjectId,
                    RepresentsStateElement::class.java,
                    true
            )

            assertThat(representsRelations).describedAs("It is exprected that every rounded rectangle in the concrete" +
                    "syntax represents one State even after it has been moved").hasSize(1)

            val relation = representsRelations.values.first()

            val state = testModelRepository[relation.b]!!

            val previousState = representationToState.getValue(graphicObjectId)

            assertThat(state.equivalent(previousState)).describedAs(
                    "Abstract Syntax State Element $state must be equivalent to previous $previousState"
            ).isTrue()
        }
    }

    lateinit var innerGrid: Table<Int, Int, StateRepresentationGenerator.StateRepresentation>

    @CouchEditSuiteTest
    @Order(5)
    fun `insert additional state concrete syntax representations`() {
        val lastCell = stateRepresentationGrid.get(gridSizeY - 1, gridSizeX - 1)

        val lastCellGraphicObject = lastCell.outerStateRectangle

        val lastCellRectangle = lastCellGraphicObject.shape as Rectangular

        val bottomRightX = lastCellRectangle.x + lastCellRectangle.w
        val bottomRightY = lastCellRectangle.y + lastCellRectangle.h

        innerGrid = stateGridGenerator.generateGridOfLabeledStates(
                additionalGridSizeX,
                additionalGridSizeY,
                bottomRightX + objectDistance,
                bottomRightY + objectDistance,
                "inserted"
        )

        val inputDiffCollection = insertAllElementsFromGrid(innerGrid)

        pt(
                inputDiffCollection,
                StateGridTest::`insert additional state concrete syntax representations`,
                "Insert ${innerGrid.cellSet().size} additional states"
        )
    }

    @CouchEditSuiteTest
    @Order(6)
    fun `should have inserted abstract syntax State objects for additional representations`() {
        assertStateRepresentationsInserted(innerGrid)
    }

    /**
     * State Rounded Rectangles that have been removed in step 7. These are stored to compare to [representationToState].
     */
    lateinit var removedBaseRectangles: Set<String>

    @CouchEditSuiteTest
    @Order(7)
    fun `remove state representations`() {
        if (toRemoveGridSizeX > gridSizeX) {
            throw RuntimeException("Cannot remove $toRemoveGridSizeX columns of Elements from a $gridSizeX column grid!")
        }

        if (toRemoveGridSizeY > gridSizeY) {
            throw RuntimeException("Cannot remove $toRemoveGridSizeY rows of Elements from a $gridSizeY row grid!")
        }

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        val newRemovedBaseRectangles = mutableSetOf<String>()

        for (i in 0 until toRemoveGridSizeX) {
            for (j in 0 until toRemoveGridSizeY) {
                val graphicObjects = stateRepresentationGrid.get(j, i)

                for (element in graphicObjects.values) {
                    inputDiffCollection.mergeCollection(testModelRepository.remove(element.id))
                }

                newRemovedBaseRectangles.add(graphicObjects.outerStateRectangle.id)
            }
        }

        removedBaseRectangles = newRemovedBaseRectangles.toSet()

        pt(
                inputDiffCollection,
                StateGridTest::`remove state representations`,
                "Remove ${inputDiffCollection.size} GraphicObjects from the top left corner"
        )
    }

    @CouchEditSuiteTest
    @Order(8)
    fun `should have removed the abstract syntax State Elements along with their concrete syntax representations`() {
        for (id in removedBaseRectangles) {
            val formerlyRepresentedState = representationToState.getValue(id)

            val testModelRepositoryState = testModelRepository[formerlyRepresentedState.id]

            assertThat(testModelRepositoryState).describedAs(
                    "RoundedRectangle $id was removed from the diagram, thus the state $formerlyRepresentedState " +
                            "previously represented by it must also be removed!").isNull()
        }
    }

    private fun insertAllElementsFromGrid(grid: Table<Int, Int, StateRepresentationGenerator.StateRepresentation>): TimedDiffCollection {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in grid.cellSet()) {
            val graphicObjects = cell.value!!.values

            for (graphicObject in graphicObjects) {
                inputDiffCollection.mergeCollection(testModelRepository.store(graphicObject))
            }
        }

        return inputDiffCollection
    }

    private fun assertStateRepresentationsInserted(grid: Table<Int, Int, StateRepresentationGenerator.StateRepresentation>): Map<String, State> {
        val representationToState = mutableMapOf<String, State>()

        for (cell in grid.cellSet()) {
            val stateBaseRectangle = cell.value!!.outerStateRectangle
            val stateLabel = cell.value!!.label!!

            val representsRelations = testModelRepository.getRelationsFromElement(
                    stateBaseRectangle.id,
                    RepresentsStateElement::class.java,
                    true
            )

            assertThat(representsRelations).describedAs("Each rounded rectangle should represent " +
                    "one abstract syntax state, got none for ${stateBaseRectangle.id}").hasSize(1)

            val relation = representsRelations.values.first()

            val representedState = testModelRepository[relation.b]!!

            assertThat(representedState.name).describedAs("State ${representedState.id} should have a name equal to " +
                    "the text in its label ${stateLabel.id}").isEqualTo((stateLabel.shape as Label).text)

            representationToState[stateBaseRectangle.id] = representedState as State
        }

        return representationToState.toMap()
    }
}
