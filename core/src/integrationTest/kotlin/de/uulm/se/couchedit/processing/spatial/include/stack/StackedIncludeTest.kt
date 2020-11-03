package de.uulm.se.couchedit.processing.spatial.include.stack

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import de.uulm.se.couchedit.BaseIntegrationTest
import de.uulm.se.couchedit.integrationtestmodel.processing.spatial.include.stack.StackedIncludeTestInfo
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.spatial.relations.Include
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.spatial.SpatialTestUtils
import de.uulm.se.couchedit.processing.spatial.controller.SpatialAbstractor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import kotlin.math.ceil

/**
 * Scenario-based test checking the correctness and performance of the [SpatialAbstractor] when working with Include #
 * relations in a Grid of Elements
 *
 * 1. Insert Grid of Elements like in the GridTest, but this time include a “stack” of smaller Elements with each one
 *
 * 2. Assert that every Element has a Include Relation from every bigger element in its stack
 *
 * 3. Move a part of each stack to the bottom of the grid
 *
 * 4. Assert that the Include relations between the bigger Elements in the stack and these stack parts have been removed
 *
 * 5. Remove one stack element from each stack
 *
 * 6. Assert that the Include relations from and to this Element have been removed
 *
 * 7. Insert a new Element on top of each stack
 *    → Assert that no Include Relations have been created towards the deleted Element
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class StackedIncludeTest(
        val gridSizeX: Int,
        val gridSizeY: Int,
        val stackDepth: Int
) : BaseIntegrationTest() {
    protected val quotientOfStackToKeep = 0.5

    protected open val stackedIncludeTestInfo = StackedIncludeTestInfo(
            gridSizeX,
            gridSizeY,
            stackDepth,
            ceil(quotientOfStackToKeep * stackDepth).toInt()
    )

    lateinit var grid: Table<Int, Int, List<PrimitiveGraphicObject<*>>>

    val systemUnderTest: SpatialAbstractor by disposableLazy {
        guiceInjector.getInstance(SpatialAbstractor::class.java)
    }

    @BeforeAll
    fun setUp() {
        val newCascadingShapes = HashBasedTable.create<Int, Int, List<PrimitiveGraphicObject<*>>>()

        for (i in 0 until gridSizeX) {
            for (j in 0 until gridSizeY) {
                val shapes = generateShapeStack(i, j)

                val baseId = "[$i,$j]"

                newCascadingShapes.put(i, j,
                        shapes.mapIndexed { index, shape ->
                            PrimitiveGraphicObject("${baseId}_$index", shape)
                        }
                )
            }
        }

        grid = newCascadingShapes
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `should return DiffCollection of spatial relations when inserting Elements`() {
        val input = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in grid.cellSet()) {
            for (element in cell.value!!) {
                input.mergeCollection(testModelRepository.store(element))
            }
        }

        val result = t(
                Action.PROCESS,
                input.size,
                StackedIncludeTest::`should return DiffCollection of spatial relations when inserting Elements`,
                "$gridSizeX x $gridSizeY stacks of size " +
                        "$stackDepth = ${stackDepth * gridSizeX * gridSizeY} PGO inserted"
        ) {
            systemUnderTest.process(input)
        }

        val appliedDiffs = t(
                Action.APPLY,
                result.size,
                StackedIncludeTest::`should return DiffCollection of spatial relations when inserting Elements`,
                "${result.size} diffs applied to the TestModelRepository"
        ) {
            testApplicator.apply(result)
        }

        Assertions.assertThat(appliedDiffs).matches { it.all { diff -> diff.affected is SpatialRelation } }
    }

    @CouchEditSuiteTest
    @Order(2)
    fun `should have inserted Include relations to all superordinate elements`() {
        for (cell in grid.cellSet()) {
            val cascade = cell.value!!

            for (i in 0 until cascade.size) {
                for (j in 0 until i) {

                    val includedId = cascade[i].id
                    val includingId = cascade[j].id

                    val includeRelations = testModelRepository.getRelationsBetweenElements(
                            includingId,
                            includedId,
                            Include::class.java,
                            false
                    )

                    assertThat(includeRelations).describedAs(
                            "Expected a single Include Relation between cascade Elements " +
                                    "$includingId and $includedId"
                    ).hasSize(1)
                }
            }
        }
    }

    /**
     * List of which Elements have been moved per stack.
     */
    protected val moveOperations = mutableListOf<StackMoveSpecification>()

    /**
     * Helper object to store a move operation
     */
    data class StackMoveSpecification(
            /**
             * The Elements that have remained untouched during this move operation
             */
            val unmovedElements: Set<ElementReference<GraphicObject<*>>>,
            /**
             * The Elements that have changed their position (out of the stack) during this move operation
             */
            val movedElements: Set<ElementReference<GraphicObject<*>>>
    )

    @CouchEditSuiteTest
    @Order(3)
    fun `should remove Include Relations when moving a sub-cascade of Elements`() {
        val diffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in grid.cellSet()) {
            val xIndex = cell.columnKey!!
            val yIndex = cell.rowKey!!

            val cascade = cell.value!!

            val bottomMostIndexToMove = (cascade.size * quotientOfStackToKeep).toInt()

            val unmovedElements = mutableSetOf<ElementReference<GraphicObject<*>>>()
            val movedElements = mutableSetOf<ElementReference<GraphicObject<*>>>()

            for (i in 0 until bottomMostIndexToMove) {
                unmovedElements.add(cascade[i].ref())
            }

            for (i in bottomMostIndexToMove until cascade.size) {
                val element = cascade[i]

                element.setContentFrom(getShapeInStack(xIndex, yIndex + gridSizeY, i))

                diffCollection.mergeCollection(testModelRepository.store(element))
                movedElements.add(element.ref())
            }

            moveOperations.add(StackMoveSpecification(unmovedElements, movedElements))
        }

        val result = t(
                Action.PROCESS,
                diffCollection.size,
                StackedIncludeTest::`should remove Include Relations when moving a sub-cascade of Elements`,
                "${diffCollection.size} Elements moved to the bottom of the Stack Grid"
        ) {
            systemUnderTest.process(diffCollection)
        }

        SpatialTestUtils.assertDiffCorrectnessAfterOperation(diffCollection, moveOperations.flatMap(
                StackMoveSpecification::movedElements
        ))

        t(
                Action.APPLY,
                result.size,
                StackedIncludeTest::`should remove Include Relations when moving a sub-cascade of Elements`,
                "${result.size} Diffs applied to the TestModelRepository"
        ) {
            this.testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(4)
    fun `should have removed the Include relations to the bigger objects in the stack`() {
        for (move in moveOperations) {
            for (movedElement in move.movedElements) {
                for (unmovedElement in move.unmovedElements) {
                    val includeRelations = testModelRepository.getRelationsBetweenElements(
                            unmovedElement.id,
                            movedElement.id,
                            Include::class.java,
                            false
                    )

                    assertThat(includeRelations).describedAs(
                            "Unmoved Element ${unmovedElement.id} " +
                                    "should not have an Include relation towards moved Element ${movedElement.id}"
                    ).hasSize(0)
                }
            }
        }
    }

    /**
     * Element removed from the grid. Will be used for re-insertion test
     */
    val removedRefs = mutableSetOf<ElementReference<PrimitiveGraphicObject<*>>>()

    @CouchEditSuiteTest
    @Order(5)
    fun `should remove only Include Relations adjacent to the removed Element upon removal`() {
        val diffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in grid.cellSet()) {
            val stack = cell.value!!

            val elementToRemoveIndex = (stack.size * quotientOfStackToKeep * 0.5).toInt()
            val element = stack[elementToRemoveIndex]

            diffCollection.mergeCollection(this.testModelRepository.remove(element.id))

            removedRefs.add(element.ref())
        }

        val result = t(
                Action.PROCESS,
                diffCollection.size,
                StackedIncludeTest::`should remove only Include Relations adjacent to the removed Element upon removal`,
                "${diffCollection.size} Elements removed"
        ) {
            systemUnderTest.process(diffCollection)
        }

        t(
                Action.APPLY,
                result.size,
                StackedIncludeTest::`should remove only Include Relations adjacent to the removed Element upon removal`,
                "Applied ${result.size} diffs to TestModelRepository"
        ) {
            this.testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(6)
    fun `should not generate Relations to removed Elements anymore when inserting new Elements`() {
        val diffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in grid.cellSet()) {
            val xPos = cell.columnKey!!
            val yPos = cell.rowKey!!

            val newObject = PrimitiveGraphicObject("top_[$xPos,$yPos]", getShapeInStack(xPos, yPos, stackDepth))

            diffCollection.mergeCollection(testModelRepository.store(newObject))
        }

        val result = t(
                Action.PROCESS,
                diffCollection.size,
                StackedIncludeTest::`should not generate Relations to removed Elements anymore when inserting new Elements`,
                "${diffCollection.size} PGO inserted"
        ) {
            systemUnderTest.process(diffCollection)
        }

        SpatialTestUtils.assertNoRelationsAdjacentToRefsInDiffCollection(
                result,
                removedRefs,
                "The SpatialAbstractor should not create Relations to elements that have been removed before"
        )
    }

    protected fun generateShapeStack(xPos: Int, yPos: Int): List<Shape> {
        val ret = mutableListOf<Shape>()

        for (i in 0 until stackDepth) {
            ret.add(getShapeInStack(xPos, yPos, i))
        }

        return ret
    }

    protected fun getShapeInStack(xPos: Int, yPos: Int, stackLevel: Int): Shape {
        val outerX = xPos * (OUTER_OBJECT_WIDTH + OBJECT_DISTANCE)
        val outerY = yPos * (OUTER_OBJECT_HEIGHT + OBJECT_DISTANCE)

        val x = outerX + stackDepth * OBJECT_DISTANCE
        val y = outerY + stackDepth * OBJECT_DISTANCE
        val w = OUTER_OBJECT_WIDTH - stackLevel * 2 * OBJECT_DISTANCE
        val h = OUTER_OBJECT_HEIGHT - stackLevel * 2 * OBJECT_DISTANCE

        return getShapeWithBounds(x, y, w, h)
    }

    abstract fun getShapeWithBounds(x: Double = 0.0, y: Double = 0.0, w: Double = 0.0, h: Double = 0.0): Shape

    companion object {
        const val OUTER_OBJECT_WIDTH = 1000
        const val OUTER_OBJECT_HEIGHT = 1000

        const val OBJECT_DISTANCE = 1.0
    }
}
