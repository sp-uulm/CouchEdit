package de.uulm.se.couchedit.processing.spatial.disjoint.grid

import com.google.common.collect.Table
import de.uulm.se.couchedit.BaseIntegrationTest
import de.uulm.se.couchedit.TestObjectGenerator
import de.uulm.se.couchedit.integrationtestmodel.processing.spatial.disjoint.grid.GridTestInfo
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.spatial.relations.BottomOfBoundary
import de.uulm.se.couchedit.model.spatial.relations.RightOfBoundary
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.spatial.SpatialTestUtils
import de.uulm.se.couchedit.processing.spatial.controller.SpatialAbstractor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * Scenario-based test case that produces a grid of [Rectangular] GraphicObjects, passes it to the
 * [SpatialAbstractor] and executes operations on this grid.
 *
 * 1. Insert a grid of [gridSizeX] x [gridSizeY] rectangular Elements returned by [getRectangularShape].
 *    All of these Elements have the same size [OBJECT_WIDTH] * [OBJECT_HEIGHT] and are OBJECT_DISTANCE
 *    apart
 *
 *    => Assert that the changes by the Processor are all Spatial Relations
 *
 * 2. Assert that every Element in the grid has a BottomOfBoundary Relation towards every Element with a smaller
 *    row number
 *
 * 3. Assert that every Element in the grid has a RightOfBoundary Relation towards every Element with a smaller
 *    column number
 *
 * 4. Move [numberOfElementsToMove] columns of Elements to the right of the grid
 *
 * 5. Now assert that every Element that has been moved in step 4 has a RightOfBoundary relation to every
 *    Element that has not been moved.
 *
 * 6. Remove an Element
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class GridTest(
        /**
         * The size of the grid, i.e. the number of Elements to be created in X direction
         */
        val gridSizeX: Int,
        /**
         * The size of the grid, i.e. the number of Elements to be created in Y direction
         */
        val gridSizeY: Int,
        /**
         * Number of objects to move out of the grid in the third step. The test will pick that number of Elements
         * from the middle of the grid.
         */
        val numberOfElementsToMove: Int
) : BaseIntegrationTest() {
    /* Initial grid parameters */
    /**
     * Height of rectangular objects to be created
     */
    protected open val objectHeight = 40.0

    /**
     * Width of rectangular objects to be created
     */
    protected open val objectWidth = 60.0

    /**
     * Distance (horizontal / vertical) of rectangular objects to be created
     */
    protected open val objectDistance = 5.0

    protected open val gridTestInfo = GridTestInfo(
            gridSizeX,
            gridSizeY,
            gridSizeX * gridSizeY,
            numberOfElementsToMove
    )

    protected val gridAreasGenerator by disposableLazy {
        GridAreasGenerator(objectHeight, objectWidth, objectDistance)
    }

    protected val testObjectGenerator: TestObjectGenerator by disposableLazy {
        TestObjectGenerator(gridAreasGenerator)
    }

    val systemUnderTest: SpatialAbstractor by disposableLazy {
        guiceInjector.getInstance(SpatialAbstractor::class.java)
    }

    lateinit var grid: Table<Int, Int, PrimitiveGraphicObject<Rectangular>>

    @BeforeAll
    fun setUp() {
        grid = testObjectGenerator.givenAGridOfRectangularGraphicObjects(
                gridSizeX,
                gridSizeY,
                this::getRectangularShape
        )

        println("Element grid of $gridSizeX x $gridSizeY = ${gridSizeX * gridSizeY} generated.")
    }

    /**
     * First, have the Processor consume the insertion of a grid of Elements (GraphicObjects).
     *
     * Apply these Diffs to the Test's ModelRepository and assert that all Diffs that were applied contain
     * SpatialRelations.
     */
    @CouchEditSuiteTest
    @Order(1)
    fun `should return DiffCollection of spatial relations when inserting Elements`() {
        val diffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        // generate input DiffCollection
        for (element in grid.values()) {
            diffCollection.mergeCollection(testModelRepository.store(element))
        }

        val result = t(
                Action.PROCESS,
                diffCollection.size,
                GridTest::`should return DiffCollection of spatial relations when inserting Elements`,
                "$gridSizeX x $gridSizeY = ${gridSizeX * gridSizeY} PGO inserted"
        ) {
            systemUnderTest.process(diffCollection)
        }

        val appliedDiffs = t(
                Action.APPLY,
                result.size,
                GridTest::`should return DiffCollection of spatial relations when inserting Elements`,
                "${result.size} diffs applied to the TestModelRepository"
        ) {
            testApplicator.apply(result)
        }

        assertThat(appliedDiffs).matches { it.all { diff -> diff.affected is SpatialRelation } }
    }

    /**
     * Second, assert that now after applying the Diffs to the Test ModelRepository (as a downstream processor would do),
     * the test ModelRepository, for every grid Element, contains BottomOfBoundary Relations to all grid Elements in
     * lower column numbers.
     */
    @CouchEditSuiteTest
    @Order(2)
    fun `should have created a BottomOfBoundary Relation for each Element in a further up position in the grid`() {
        for (cell in grid.cellSet()) {
            val i = cell.rowKey!!
            val element = cell.value!!

            // assert that for all Elements with a row number less than the current Element, there must be a BottomOf
            // relation from the current Element to the other Element
            for (otherI in 0 until i) {
                for (rowElement in grid.row(otherI).values) {
                    assertThat(testModelRepository.getRelationsBetweenElements(
                            element.id,
                            rowElement.id,
                            BottomOfBoundary::class.java,
                            true
                    )).hasSize(1)
                }
            }
        }
    }

    /**
     * Third, assert that now after applying the Diffs to the Test ModelRepository (as a downstream processor would do),
     * the test ModelRepository, for every grid Element, contains RightOfBoundary Relations towards all grid Elements in
     * lower row numbers.
     */
    @CouchEditSuiteTest
    @Order(3)
    fun `should have created a RightOfBoundary Relation for each Element in a further left position in the grid`() {
        for (cell in grid.cellSet()) {
            val j = cell.columnKey!!
            val element = cell.value!!

            // assert that for all Elements with a column number less than the current Element, there must be a bottomOf
            // relation
            for (otherJ in 0 until j) {
                for (columnElement in grid.column(otherJ).values) {
                    assertThat(testModelRepository.getRelationsBetweenElements(
                            element.id,
                            columnElement.id,
                            RightOfBoundary::class.java,
                            true
                    )).hasSize(1)
                }
            }
        }
    }

    /**
     * Set of Elements that have been moved in Step 4.
     */
    protected val movedElements = mutableSetOf<ElementReference<GraphicObject<*>>>()

    @CouchEditSuiteTest
    @Order(4)
    fun `should produce SpatialRelation diffs only towards non-affected Elements when uniformly moving Elements out of the grid`() {
        val changedElements = gridAreasGenerator.moveElementsToRightOfGrid(
                grid,
                numberOfElementsToMove,
                {
                    with(it.content) {
                        return@with GridAreasGenerator.Area(x, y, w, h)
                    }
                },
                { element, (newX, newY, newW, newH) ->
                    with(element.content) {
                        x = newX
                        y = newY
                        w = newW
                        h = newH
                    }

                    element
                }
        )

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()
        for (element in changedElements) {
            inputDiffCollection.mergeCollection(testModelRepository.store(element))
            movedElements.add(element.ref())
        }

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                GridTest::`should produce SpatialRelation diffs only towards non-affected Elements when uniformly moving Elements out of the grid`,
                "${inputDiffCollection.size} Elements moved to the right of the grid.") {
            systemUnderTest.process(inputDiffCollection)
        }

        SpatialTestUtils.assertDiffCorrectnessAfterOperation(result, movedElements)

        val applied = t(
                Action.APPLY,
                result.size,
                GridTest::`should produce SpatialRelation diffs only towards non-affected Elements when uniformly moving Elements out of the grid`,
                "${inputDiffCollection.size} Elements moved to the right of the grid.") {
            testApplicator.apply(result)
        }

        assertAllAffectedElementsSpatial(applied)
    }

    @CouchEditSuiteTest
    @Order(5)
    fun `should have generated RightOfBoundary relations for all Elements that have been moved`() {
        for (ref in movedElements) {
            for (cell in grid.cellSet()) {
                val element = cell.value ?: continue

                if (element.ref() in movedElements) {
                    continue
                }

                val relations = testModelRepository.getRelationsBetweenElements(ref.id, element.id, RightOfBoundary::class.java, false)

                assertThat(relations).describedAs(
                        "There must be exactly one RightOfBoundary relation from every moved Element " +
                                "to every non-moved Element"
                ).hasSize(1)
            }
        }
    }

    val removedRefs = mutableSetOf<ElementReference<PrimitiveGraphicObject<*>>>()

    @CouchEditSuiteTest
    @Order(6)
    fun `should only generate remove diffs for Relations adjacent to that Element if an Element is removed`() {
        val elementToRemove = grid.get(0, 0)!!

        val inputDiffs = testModelRepository.remove(elementToRemove.id)

        val result = t(
                Action.PROCESS,
                inputDiffs.size,
                GridTest::`should only generate remove diffs for Relations adjacent to that Element if an Element is removed`,
                "Deleting ${inputDiffs.size} Elements") {
            systemUnderTest.process(inputDiffs)
        }

        removedRefs.add(elementToRemove.ref())

        SpatialTestUtils.assertDiffCorrectnessAfterOperation(result, removedRefs)

        t(
                Action.APPLY,
                result.size,
                GridTest::`should only generate remove diffs for Relations adjacent to that Element if an Element is removed`,
                "Deleting ${result.size} Elements"
        ) {
            testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(7)
    fun `should not generate Relations to removed Elements anymore when inserting new Elements`() {
        val oldElement = grid.get(0, 0)!!

        val elementToAdd = PrimitiveGraphicObject("newElement", oldElement.content)

        val inputDiffs = testModelRepository.store(elementToAdd)

        val result = t(
                Action.PROCESS,
                inputDiffs.size,
                GridTest::`should not generate Relations to removed Elements anymore when inserting new Elements`,
                "Inserting ${inputDiffs.size} Elements") {
            systemUnderTest.process(inputDiffs)
        }

        SpatialTestUtils.assertDiffCorrectnessAfterOperation(result, setOf(elementToAdd.ref()))

        SpatialTestUtils.assertNoRelationsAdjacentToRefsInDiffCollection(
                result,
                removedRefs,
                "The spatialAbstractor should not create Relations to Elements that have been removed before"
        )

        t(
                Action.APPLY,
                result.size,
                GridTest::`should not generate Relations to removed Elements anymore when inserting new Elements`,
                "Inserting ${inputDiffs.size} Elements") {
            testApplicator.apply(result)
        }
    }

    protected fun assertAllAffectedElementsSpatial(diffs: DiffCollection) {
        assertThat(diffs)
                .describedAs("All Diffs are expected to concern SpatialRelations!")
                .matches { diffsToMatch -> diffsToMatch.all { it.affected is SpatialRelation } }
    }

    abstract fun getRectangularShape(x: Double = 0.0, y: Double = 0.0, w: Double = 0.0, h: Double = 0.0): Rectangular
}
