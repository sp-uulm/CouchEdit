package de.uulm.se.couchedit.processing.spatial.intersect

import de.uulm.se.couchedit.BaseIntegrationTest
import de.uulm.se.couchedit.integrationtestmodel.processing.spatial.intersect.CascadingIntersectTestInfo
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.spatial.relations.Intersect
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.spatial.SpatialTestUtils
import de.uulm.se.couchedit.processing.spatial.controller.SpatialAbstractor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CascadingIntersectTest(val cascadeSize: Int, val numberOfElementsToMove: Int) : BaseIntegrationTest() {
    /**
     * Width of the cascading objects to be created
     */
    val rectangularObjectWidth = 60.0

    /**
     * Height of the cascading objects to be created
     */
    val rectangularObjectHeight = 60.0

    /**
     * Distance that the objects are overlapping in both X and Y dimension
     */
    val objectOverlapDistance = 20.0

    val systemUnderTest: SpatialAbstractor by disposableLazy {
        guiceInjector.getInstance(SpatialAbstractor::class.java)
    }

    protected open val cascadingIntersectTestInfo = CascadingIntersectTestInfo(cascadeSize, numberOfElementsToMove)

    abstract fun getRectangularShape(x: Double = 0.0, y: Double = 0.0, w: Double = 0.0, h: Double = 0.0): Rectangular

    lateinit var cascade: List<PrimitiveGraphicObject<out Rectangular>>

    @BeforeAll
    fun setUp() {
        cascade = givenCascadeOfRectangularObjects(cascadeSize)
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `should insert spatial Relations when inserting a cascade of mutually-intersecting GraphicObjects`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (element in cascade) {
            inputDiffCollection.mergeCollection(testModelRepository.store(element))
        }

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                CascadingIntersectTest::`should insert spatial Relations when inserting a cascade of mutually-intersecting GraphicObjects`,
                "Insert ${inputDiffCollection.size} cascading Rectangular Elements"
        ) {
            systemUnderTest.process(inputDiffCollection)
        }

        t(
                Action.APPLY,
                result.size,
                CascadingIntersectTest::`should insert spatial Relations when inserting a cascade of mutually-intersecting GraphicObjects`,
                "Apply ${result.size} SpatialRelations to the TestModelRepository"
        ) {
            testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(2)
    fun `should have inserted an Intersect relation towards the previous and the next Element`() {
        for (i in 0 until cascade.size) {
            val element = cascade[i]

            val intersectRelations = testModelRepository.getRelationsAdjacentToElement(
                    element.id,
                    Intersect::class.java,
                    false
            ).values.toMutableList()

            if (i > 0) {
                val previousId = cascade[i - 1].id

                val relationToPrevious = intersectRelations.find { previousId in setOf(it.a.id, it.b.id) }

                assertThat(relationToPrevious).describedAs(
                        "Element ${element.id} must have an Intersect relation to its preceding Element"
                ).isNotNull()

                intersectRelations.remove(relationToPrevious)
            }

            if (i < cascade.size - 1) {
                val nextId = cascade[i + 1].id

                val relationToNext = intersectRelations.find { nextId in setOf(it.a.id, it.b.id) }

                assertThat(relationToNext).describedAs(
                        "Element ${element.id} must have an Intersect relation to its next Element"
                ).isNotNull()

                intersectRelations.remove(relationToNext)
            }

            assertThat(intersectRelations).describedAs(
                    "Element ${element.id} should not have Intersect relations to Elements other than the previous" +
                            "and next ones in the cascade"
            ).isEmpty()
        }
    }

    /**
     * Stores the GraphicObjects that are moved in step 3
     */
    lateinit var movedElements: List<PrimitiveGraphicObject<out Rectangular>>

    /**
     * Stores the distance by which the GraphicObjects are moved in y direction in step 3
     */
    var yDistanceMoved: Double = 0.0

    var firstMovedIndex: Int = 0
    var lastMovedIndex: Int = 0

    @CouchEditSuiteTest
    @Order(3)
    fun `should produce SpatialRelation diffs only towards the non-moved Elements when moving Elements out of the cascade`() {
        val newMovedElements = mutableListOf<PrimitiveGraphicObject<out Rectangular>>()

        val firstIndexToMove = ((cascadeSize - numberOfElementsToMove) / 2) - 1

        val firstElementToMove = cascade[firstIndexToMove]
        val yToMoveTo = cascade.last().content.y + rectangularObjectHeight + objectOverlapDistance

        yDistanceMoved = yToMoveTo - firstElementToMove.content.y

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (i in firstIndexToMove until firstIndexToMove + numberOfElementsToMove) {
            val element = cascade[i]

            element.content.y += yDistanceMoved

            newMovedElements.add(element)

            inputDiffCollection.mergeCollection(testModelRepository.store(element))
        }

        movedElements = newMovedElements

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                CascadingIntersectTest::`should produce SpatialRelation diffs only towards the non-moved Elements when moving Elements out of the cascade`,
                "Move ${inputDiffCollection.size} cascading Elements") {
            systemUnderTest.process(inputDiffCollection)
        }

        val movedElementRefs = movedElements.map(GraphicObject<*>::ref)

        SpatialTestUtils.assertDiffCorrectnessAfterOperation(result, movedElementRefs)

        for (diff in result) {
            val affected = diff.affected as? SpatialRelation ?: continue

            assertThat(affected.a !in movedElementRefs || affected.b !in movedElementRefs)
                    .describedAs("When moving multiple Elements together, the SpatialAbstractor should not change " +
                            "Spatial relations between them, but did nevertheless for (${affected.a}, ${affected.b})").isTrue()
        }

        t(
                Action.APPLY,
                result.size,
                CascadingIntersectTest::`should produce SpatialRelation diffs only towards the non-moved Elements when moving Elements out of the cascade`,
                "Apply ${result.size} diffs after moving Elements"
        ) {
            testApplicator.apply(inputDiffCollection)
        }
    }

    /**
     * Index of the last Element before the set of moved Elements
     */
    val elementBeforeMovedIndex = firstMovedIndex - 1

    /**
     * Index of the first element after the set of moved Elements
     */
    val elementAfterMovedIndex = lastMovedIndex + 1

    @CouchEditSuiteTest
    @Order(4)
    fun `should have removed Intersect relations between the moved Elements and their neighboring Elements`() {
        if (elementBeforeMovedIndex < 0) {
            return
        }

        if (elementAfterMovedIndex >= cascade.size) {
            return
        }

        val firstIntersectRelations = getIntersectRelationsBetweenElementBeforeMovedIndexAndFirstMovedElement()

        assertThat(firstIntersectRelations).describedAs(
                "There should be no Intersect relation between non-moved Element $elementBeforeMovedIndex" +
                        "and moved Element $firstMovedIndex, but found ${firstIntersectRelations.size}"
        ).isEmpty()

        val lastIntersectRelations = getIntersectRelationsBetweenElementAfterLastMovedIndexAndLastMovedElement()

        assertThat(lastIntersectRelations).describedAs(
                "There should be no Intersect relation between non-moved Element $elementAfterMovedIndex" +
                        "and moved Element $lastMovedIndex, but found ${lastIntersectRelations.size}"
        ).isEmpty()
    }

    /**
     * Last Element in the cascade
     */
    lateinit var lastElement: PrimitiveGraphicObject<out Rectangular>

    /**
     * Element to be inserted behind the [lastElement] in the cascade
     */
    val newElement = getCascadeElement("newElement", cascadeSize)

    @CouchEditSuiteTest
    @Order(5)
    fun `should produce an Intersect relation when inserting a new cascade Element`() {
        lastElement = cascade[cascade.size - 1]

        val inputDiffCollection = testModelRepository.store(newElement)

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                CascadingIntersectTest::`should produce an Intersect relation when inserting a new cascade Element`,
                "Inserted new Element at the end of the cascade") {
            systemUnderTest.process(inputDiffCollection)
        }

        for (diff in result) {
            val affected = diff.affected as? SpatialRelation ?: continue

            assertThat(affected.a == newElement.ref() || affected.b == newElement.ref()).describedAs(
                    "When inserting an Element, no Relations to unrelated Elements should change"
            )
        }

        t(
                Action.APPLY,
                result.size,
                CascadingIntersectTest::`should produce an Intersect relation when inserting a new cascade Element`,
                "Apply ${result.size} diffs after inserting new Element at the end of the cascade") {
            testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(6)
    fun `should produce Spatial Relations when moving the Elements back into the cascade`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (element in movedElements) {
            element.content.y -= yDistanceMoved

            inputDiffCollection.mergeCollection(testModelRepository.store(element))
        }

        val result = t(
                Action.PROCESS,
                inputDiffCollection.size,
                CascadingIntersectTest::`should produce Spatial Relations when moving the Elements back into the cascade`,
                "Moved ${movedElements.size} Elements back into the cascade") {
            systemUnderTest.process(inputDiffCollection)
        }

        t(
                Action.APPLY,
                result.size,
                CascadingIntersectTest::`should produce Spatial Relations when moving the Elements back into the cascade`,
                "Apply ${result.size} Diffs to the Test Model Repository after moving Elements back into the cascade"
        ) {
            testApplicator.apply(result)
        }
    }

    @CouchEditSuiteTest
    @Order(7)
    fun `should have reinserted the spatial relations from the moved-back Elements to the neighboring Elements`() {
        if (elementBeforeMovedIndex < 0) {
            return
        }

        if (elementAfterMovedIndex >= cascade.size) {
            return
        }

        val firstIntersectRelations = getIntersectRelationsBetweenElementBeforeMovedIndexAndFirstMovedElement()

        assertThat(firstIntersectRelations).describedAs(
                "An Intersect relation should have been reinserted between Element $elementBeforeMovedIndex" +
                        "and previously moved Element $firstMovedIndex, but found ${firstIntersectRelations.size}"
        ).hasSize(1)

        val lastIntersectRelations = getIntersectRelationsBetweenElementAfterLastMovedIndexAndLastMovedElement()

        assertThat(lastIntersectRelations).describedAs(
                "An Intersect relation should have been reinserted between Element $elementAfterMovedIndex" +
                        "and previously moved Element $lastMovedIndex, but found ${lastIntersectRelations.size}"
        ).hasSize(1)
    }

    protected fun getIntersectRelationsBetweenElementBeforeMovedIndexAndFirstMovedElement(): ElementQueryResult<Intersect> {
        return testModelRepository.getRelationsBetweenElements(
                cascade[elementBeforeMovedIndex].id,
                cascade[firstMovedIndex].id,
                Intersect::class.java,
                true
        )
    }

    protected fun getIntersectRelationsBetweenElementAfterLastMovedIndexAndLastMovedElement(): ElementQueryResult<Intersect> {
        return testModelRepository.getRelationsBetweenElements(
                cascade[lastMovedIndex].id,
                cascade[elementAfterMovedIndex].id,
                Intersect::class.java,
                true
        )
    }

    protected fun givenCascadeOfRectangularObjects(size: Int): List<PrimitiveGraphicObject<out Rectangular>> {
        val cascade = mutableListOf<PrimitiveGraphicObject<out Rectangular>>()

        for (i in 0 until size) {
            cascade.add(getCascadeElement(i.toString(), i))
        }

        return cascade
    }

    protected fun getCascadeElement(id: String, index: Int): PrimitiveGraphicObject<out Rectangular> {
        val x = index * (rectangularObjectWidth - objectOverlapDistance)
        val y = index * (rectangularObjectHeight - objectOverlapDistance)

        val shape = getRectangularShape(x, y, rectangularObjectWidth, rectangularObjectHeight)

        return PrimitiveGraphicObject(id, shape)
    }
}
