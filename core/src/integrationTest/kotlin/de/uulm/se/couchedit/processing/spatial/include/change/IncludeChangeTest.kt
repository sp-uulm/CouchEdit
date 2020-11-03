package de.uulm.se.couchedit.processing.spatial.include.change

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import de.uulm.se.couchedit.BaseIntegrationTest
import de.uulm.se.couchedit.TestObjectGenerator
import de.uulm.se.couchedit.integrationtestmodel.processing.spatial.include.change.IncludeChangeTestInfo
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.spatial.relations.Include
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.spatial.SpatialTestUtils
import de.uulm.se.couchedit.processing.spatial.controller.SpatialAbstractor
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTestFactory
import kotlin.reflect.KFunction

/**
 * Scenario-based test that tests correctness and performance of the SpatialAbstractor when moving
 * a small "inner" set of Elements in a big "outer" set of Elements so that the "inner" Elements are always
 * contained within exactly one Element of the "outer" set and are always moved together.
 *
 * 1. Create "outer" grid of Elements
 * 2. Create one additional "inner" grid of Elements, this grid being included in the top left one
 * 3. Now move this Element to be included in other Elements
 * 4. Assert that old Include Relation has been removed
 * 5. Assert that new Include Relation has been added
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IncludeChangeTest(
        /**
         * The size of the grid, i.e. the number of Elements to be created in X direction
         */
        protected val outerGridSizeX: Int,
        /**
         * The size of the grid, i.e. the number of Elements to be created in Y direction
         */
        protected val outerGridSizeY: Int,
        protected val innerGridSizeX: Int,
        protected val innerGridSizeY: Int
) : BaseIntegrationTest() {
    protected val outerTestObjectGenerator by disposableLazy {
        TestObjectGenerator(GridAreasGenerator(outerGridObjectHeight, outerGridObjectWidth, outerGridObjectDistance))
    }

    protected val outerGrid by disposableLazy {
        outerTestObjectGenerator.givenAGridOfRectangularGraphicObjects(
                outerGridSizeX,
                outerGridSizeY,
                this::getRectangularShape
        )
    }

    protected lateinit var innerGrid: Table<Int, Int, PrimitiveGraphicObject<Rectangular>>

    protected open val includeChangeTestInfo = IncludeChangeTestInfo(
            outerGridSizeX,
            outerGridSizeY,
            outerGridSizeX * outerGridSizeY,
            innerGridSizeX,
            innerGridSizeY,
            innerGridSizeX * innerGridSizeY
    )

    /* Initial grid parameters */
    /**
     * Height of rectangular objects to be created
     */
    protected open val outerGridObjectHeight = 40.0

    /**
     * Width of rectangular objects to be created
     */
    protected open val outerGridObjectWidth = 60.0

    /**
     * Distance (horizontal / vertical) of the "outer grid" of rectangular objects to be created
     */
    protected open val outerGridObjectDistance = 5.0

    /**
     * Distance that the inner grid Elements
     */
    protected open val innerObjectDistance = 2.0

    val systemUnderTest: SpatialAbstractor by disposableLazy {
        guiceInjector.getInstance(SpatialAbstractor::class.java)
    }

    abstract fun getRectangularShape(x: Double = 0.0, y: Double = 0.0, w: Double = 0.0, h: Double = 0.0): Rectangular

    @BeforeAll
    fun setUp() {
        println("Outer Element grid of $outerGridSizeX x $outerGridSizeY = ${outerGridSizeX * outerGridSizeY} generated.")

        val insertDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in outerGrid.cellSet()) {
            val element = cell.value!!

            insertDiffCollection.mergeCollection(testModelRepository.store(element))
        }

        val result = systemUnderTest.process(insertDiffCollection)

        testApplicator.apply(result)
    }

    lateinit var firstParentObject: PrimitiveGraphicObject<*>
    protected val innerElementReferences = mutableSetOf<ElementReference<GraphicObject<*>>>()

    @CouchEditSuiteTest
    @Order(1)
    fun `should insert Spatial relations when inserting inner Elements`() {
        firstParentObject = outerGrid.get(0, 0)

        val initialShapes = getInnerGridShapes(0, 0)

        val newInnerGrid = HashBasedTable.create<Int, Int, PrimitiveGraphicObject<Rectangular>>()

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in initialShapes.cellSet()) {
            val x = cell.columnKey!!
            val y = cell.rowKey!!

            val shape = cell.value!!

            val id = "inner_[$x,$y]"

            val go = PrimitiveGraphicObject(id, shape)

            innerElementReferences.add(go.ref())

            newInnerGrid.put(y, x, go)

            inputDiffCollection.mergeCollection(testModelRepository.store(go))
        }

        innerGrid = newInnerGrid

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                IncludeChangeTest::`should insert Spatial relations when inserting inner Elements`,
                "Insert ${inputDiffCollection.size} PGO for the 'inner grid'"
        ) {
            systemUnderTest.process(inputDiffCollection)
        }


        for (diff in result) {
            val affected = diff.affected as? SpatialRelation ?: continue

            assertThat(affected.a in innerElementReferences || affected.b in innerElementReferences).describedAs(
                    "All spatial relations generated must be related to one of the inserted Elements"
            ).isTrue()
        }

        t(
                Action.APPLY,
                result.size,
                IncludeChangeTest::`should insert Spatial relations when inserting inner Elements`,
                "Apply ${result.size} Diffs to the TestModelRepository"
        ) {
            testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(2)
    fun `should have created Include relations from each of the inner grid elements to the top-left outer grid Element`() {
        assertIncludeRelationFromInnerGridElementsToOuterGridElement(firstParentObject.id)
    }

    /**
     * TestFactory that creates one test per [outerGrid] Element where the [innerGrid] Elements are moved into that
     * Element by the [testIncludeMove] function.
     *
     * Excluded here is the (0, 0) position, as the elements are already there as per tests 1 and 2
     */
    @CouchEditSuiteTestFactory
    @Order(3)
    fun `should generate correct include tests when moving inner grid Elements to each outer grid Element`(): List<DynamicTest> {
        return outerGrid.cellSet().filter {
            it.rowKey != 0 || it.columnKey != 0
        }.map { cell ->
            val yPos = cell.rowKey!!
            val xPos = cell.columnKey!!
            DynamicTest.dynamicTest("Move inner grid to [$xPos, $yPos]") {
                testIncludeMove(
                        xPos,
                        yPos,
                        IncludeChangeTest::`should generate correct include tests when moving inner grid Elements to each outer grid Element`
                )
            }
        }
    }

    @CouchEditSuiteTest
    @Order(4)
    fun `should re-create correct Include relations when moving back to the top-left outer grid Element`() {
        testIncludeMove(0, 0, IncludeChangeTest::`should re-create correct Include relations when moving back to the top-left outer grid Element`)
    }

    /**
     * Test method for the generated tests that moves the inner grid of Elements to the outer grid Element with
     * the given [targetOuterPosX]. Then it is asserted that:
     *
     * * Each "inner" grid Element has exactly one Include relation towards it
     * * That Include relation originates from the outer Element where the inner Elements should now be contained in
     * *
     */
    fun testIncludeMove(targetOuterPosX: Int, targetOuterPosY: Int, originalMethod: KFunction<*>) {
        val outerElement = outerGrid.get(targetOuterPosY, targetOuterPosX)!!

        // get the shapes for the new position of the inner grid Elements
        val shapes = getInnerGridShapes(targetOuterPosX, targetOuterPosY)

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (cell in shapes.cellSet()) {
            val shape = cell.value!!

            // get the Element which corresponds to the Shape contained in the cell
            val element = innerGrid.get(cell.rowKey, cell.columnKey)

            element.setContentFrom(shape)

            inputDiffCollection.mergeCollection(testModelRepository.store(element))
        }

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                originalMethod,
                "Inner grid moved to [$targetOuterPosX,$targetOuterPosY]"
        ) {
            systemUnderTest.process(inputDiffCollection)
        }

        SpatialTestUtils.assertDiffCorrectnessAfterOperation(result, innerElementReferences)

        t(
                Action.APPLY,
                result.size,
                originalMethod,
                "Apply ${result.size} diffs to TestModelRepo"
        ) {
            testApplicator.apply(result)
        }

        for (cell in innerGrid.cellSet()) {
            val innerElementId = cell.value!!.id

            val includeRelations = testModelRepository.getRelationsToElement(
                    innerElementId,
                    Include::class.java,
                    false
            )

            assertThat(includeRelations).describedAs(
                    "After moving, every inner Element must have exactly one Include relation" +
                            "towards it, but found ${includeRelations.size} " +
                            "from ${outerElement.id} to $innerElementId"
            ).hasSize(1)

            assertThat(includeRelations.values.first().a).describedAs("The inner Element $innerElementId must now have" +
                    "an Include relation towards the outer Element ${outerElement.id}").isEqualTo(outerElement.ref())
        }
    }

    /**
     * Generates the shapes of the inner grid so that they are contained in the outer grid Element with grid position
     * ([outerPosX],[outerPosY])
     */
    protected fun getInnerGridShapes(outerPosX: Int, outerPosY: Int): Table<Int, Int, Rectangular> {
        val ret = HashBasedTable.create<Int, Int, Rectangular>()

        val outerObject = outerGrid.get(outerPosY, outerPosX)

        val innerMinX = outerObject.content.x
        val innerMinY = outerObject.content.y
        val innerMaxX = outerObject.content.x + outerObject.content.w
        val innerMaxY = outerObject.content.y + outerObject.content.h

        val innerElementWidth = (innerMaxX - innerMinX - ((innerGridSizeX + 1) * innerObjectDistance)) / innerGridSizeX
        val innerElementHeight = (innerMaxY - innerMinY - ((innerGridSizeY + 1) * innerObjectDistance)) / innerGridSizeY

        for (i in 0 until innerGridSizeX) {
            for (j in 0 until innerGridSizeY) {
                val x = innerMinX + innerObjectDistance + i * (innerElementWidth + innerObjectDistance)
                val y = innerMinY + innerObjectDistance + j * (innerElementHeight + innerObjectDistance)

                val shape = getRectangularShape(x, y, innerElementWidth, innerElementHeight)

                ret.put(j, i, shape)
            }
        }

        return ret
    }

    /**
     * Asserts that in the [testModelRepository], there are Include relations from each Element of the [innerGrid]
     * towards the Element with the specified [outerGridElementId]
     */
    protected fun assertIncludeRelationFromInnerGridElementsToOuterGridElement(outerGridElementId: String) {
        for (cell in innerGrid.cellSet()) {
            val innerGridElement = cell.value!!

            val includeRelations = testModelRepository.getRelationsBetweenElements(
                    outerGridElementId,
                    innerGridElement.id,
                    Include::class.java,
                    false
            )

            assertThat(includeRelations).describedAs(
                    "There must be one Include Relation from the outer Element to each inner Element, " +
                            "but found none from $outerGridElementId to ${innerGridElement.id}"
            ).isNotEmpty()
        }
    }
}
