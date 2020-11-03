package de.uulm.se.couchedit.statecharts.scenarios.compartment.recursive

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.statecharts.model.couch.elements.OrthogonalRegion
import de.uulm.se.couchedit.statecharts.model.couch.elements.State
import de.uulm.se.couchedit.statecharts.model.couch.relations.ContainsRegion
import de.uulm.se.couchedit.statecharts.model.couch.relations.ParentOf
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsOrthogonalState
import de.uulm.se.couchedit.statecharts.scenarios.BaseStatechartScenario
import de.uulm.se.couchedit.statecharts.testdata.OrthogonalStateLineRepresentation.LineOrientation.HORIZONTAL
import de.uulm.se.couchedit.statecharts.testdata.OrthogonalStateLineRepresentation.LineOrientation.VERTICAL
import de.uulm.se.couchedit.statecharts.testdata.SplittingOrthogonalStateLineGenerator
import de.uulm.se.couchedit.statecharts.testdata.SplittingOrthogonalStateLineGenerator.SplitResult
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator.StateRepresentation
import de.uulm.se.couchedit.statecharts.testmodel.RecursiveOrthogonalStateTestDetails
import de.uulm.se.couchedit.systemtestutils.test.SystemTestProcessor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order
import org.locationtech.jts.geom.prep.PreparedGeometry
import org.locationtech.jts.geom.prep.PreparedGeometryFactory
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow

/**
 * Scenario for testing the generation of sub-compartments and their detection as orthogonal states.
 *
 * 1. Generate a state representation [outerStateRepresentation] which spans [outermostSize] in both width and height.
 *    Along with it, use the [compartmentLineGenerator] to recursively divide the State representation into
 *    sub-compartments:
 *
 *    The top or left compartment of each splitting step is referred to as the "first" compartment in the following,
 *    the bottom or right compartment as the "second".
 *
 *    The following is repeated [depth] times:
 *    The second one of the areas divided in the last step will be divided in two halves by another line.
 *    If the last division was horizontal, the area is divided vertically, and vice versa.
 *
 *    This is done so that (apart from the first splitting line) no line would constitute a split by itself, rather
 *    needing the line inserted in the previous step(s).
 *
 *    This whole graphical constellation is then inserted in a single DiffCollection, each line associated with a
 *    LineAttributes object making it dashed so that it is recognized as an orthogonal state divider.
 *
 * 2. Verify correct insertion of the "core" CompartmentHotSpotDefinitions -
 *    Assert that for each "splitting step", two CompartmentHotSpotDefinitions have been inserted which:
 *      * depend on the splitting line on its "b" side and
 *      * have the "second" next-higher compartment as their [CompartmentHotSpotDefinition.splitCompartment] value
 *      * If the split was horizontal, the [CompartmentHotSpotDefinition.index]es have to be (0, 0) and (0, 1), if
 *        the split was vertical, the indexes have to be (0, 0) and (1, 0)
 *
 * 3. Verify correct insertion of the OrthogonalStates - assert that for all splittings but the last, only for the
 *    "first" CompartmentHotSpotDefinition of each splitting step, an [OrthogonalRegion] Abstract Syntax Element has
 *    been inserted, and no OrthogonalRegion Element has been inserted for the "second" CompartmentHotSpotDefinition.
 *
 *    This is the correct behavior as Compartments which are further split up should not be marked as an
 *    OrthogonalRegion - the number of OrthogonalRegions should be the same as "Regions" visible in the FrontEnd.
 *
 *    Only for the last "splitting step", both generated CompartmentHotSpotDefinitions should be recognized as
 *    OrthogonalRegions.
 *
 * 4. Now generate a set of State concrete syntax representations where each is contained in the "first" Compartment
 *    (which represents an [OrthogonalRegion] as checked in Step 3) and insert them in one DiffCollection.
 *    Also create one State Concrete Syntax representation that is contained in the "second" compartment of the
 *    last split step.
 *
 *    This is done to both check that the recursive [OrthogonalRegion]s' concrete syntax / geometric "regions" are
 *    calculated correctly and that Contains Relations from the Core are correctly translated to [ParentOf] Relations
 *    in the Abstract Syntax.
 *
 * 5. Assert that for each of the inserted states, a [ParentOf] relation exists towards the OrthogonalState of the
 *    split step it has been associated to
 *
 * 6. Remove the state representation from the outermost "first" compartment. This is to prepare for step 7.
 *
 * 7. Add a new line that splits the outermost "first" compartment into two sub-compartments.
 *    This is to measure the time it takes to process an insertion of a new Compartment by the user.
 *
 * 8. Assert that two new CompartmentHotSpotDefinitions have been created depending on the outermost "first" compartment.
 *
 * 9. Assert that the operation in step 7 has resulted in
 *      * Each of the "new" compartments resulting from the newly inserted line representing one [OrthogonalRegion]
 *      * The "old" first Compartment of the first split level no longer representing an [OrthogonalRegion]
 *
 * 10. Move the last split step's line out of the outer state's shape.
 *     This is to check the correct detection if a Compartment is no longer valid by the core components and the correct
 *     change in ParentOf abstract syntax relation that should follow from that.
 *
 *     We do this via a move and not a simple delete as delete is always easier to detect and as the
 *     CompartmentHotSpotDefinition is itself a Relation dependent on the line it will be automatically deleted by the
 *     ModelRepository if the line is deleted.
 *
 * 11. Assert that the OrthogonalStates for both previous compartments dependent on the last split step's line have been
 *     deleted out of the abstract syntax.
 *
 * 12. Assert that the States which previously were children of those OrthogonalStates now have a correct ParentOf relation
 *     coming from the [OrthogonalRegion] represented by the "second" CompartmentHotSpotDefinition of the next-higher
 *     split step
 */
@Suppress("FunctionName")
class RecursiveOrthogonalStateTest(private val minimumInnerSize: Double, private val depth: Int) : BaseStatechartScenario() {
    override val testInstanceInfo = TestInstanceInfo(
            "ROST",
            "RecursiveOrthogonalStateTest: Depth $depth",
            RecursiveOrthogonalStateTestDetails(depth, depth + 1)
    )
    private val roundedEdgeSize: Double = 10.0

    /**
     * Distance that all compartment-splitting lines should have from the outer border of their Area.
     */
    private val compartmentLineMargin: Double = 3.0

    override val systemTestProcessor by disposableLazy {
        SystemTestProcessor(listOf(
                GraphicObject::class.java,
                CompartmentHotSpotDefinition::class.java,
                Represents::class.java,
                State::class.java,
                OrthogonalRegion::class.java,
                ContainsRegion::class.java,
                ParentOf::class.java
        ))
    }

    private val stateRepresentationGenerator by disposableLazy {
        StateRepresentationGenerator(roundedEdgeSize)
    }

    private val outerArea by disposableLazy {
        val outerWidth = minimumInnerSize * 2.0.pow(ceil(depth / 2.0))

        GridAreasGenerator.Area(0.0, 0.0, outerWidth, outerWidth)
    }

    private val outerStateRepresentation by disposableLazy {
        stateRepresentationGenerator.getStateRepresentationFrom(outerArea, "outer", StateRepresentationGenerator.LabelPosition.None)
    }

    private val compartmentLineGenerator by disposableLazy {
        SplittingOrthogonalStateLineGenerator(compartmentLineMargin)
    }

    /**
     * [SplitResult] objects (the first part of the Triple) recursively in the [outerStateRepresentation] so that:
     *
     * * the second part of the triple is a [StateRepresentation] which fits into the
     *   upper or left orthogonal state
     * * the lower or right orthogonal state is further split up again into two sub-compartments by the line in the
     *   next list Element. For the last split level, the third part of the triple is a [StateRepresentation] contained in the right /
     *   lower compartment.
     */
    private val splits by disposableLazy {
        val ret = mutableListOf<
                Triple<SplitResult,
                        StateRepresentation,
                        StateRepresentation?>
                >()

        var areaToSplit = outerArea
        var currentOrientation = VERTICAL

        for (i in 0 until depth) {
            val result = compartmentLineGenerator.split("splitLine_$i", areaToSplit, currentOrientation)

            val (firstArea, secondArea) = result.areas
            val margin = 0.05 * min(firstArea.w, firstArea.h)

            val stateRepresentation = stateRepresentationGenerator.getStateRepresentationFrom(
                    GridAreasGenerator.reduceAreaByMargin(firstArea, margin),
                    "state_firstArea_$i"
            )

            val stateRepresentation2 = if (i == depth - 1) stateRepresentationGenerator.getStateRepresentationFrom(
                    GridAreasGenerator.reduceAreaByMargin(secondArea, margin),
                    "state_secondArea_$i"
            ) else null

            areaToSplit = secondArea

            currentOrientation = if (currentOrientation == VERTICAL) HORIZONTAL else VERTICAL

            ret.add(Triple(result, stateRepresentation, stateRepresentation2))
        }

        return@disposableLazy ret
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `insert orthogonal state representations`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        inputDiffCollection.mergeCollection(testModelRepository.store(outerStateRepresentation.outerStateRectangle))

        for ((splitDefinition, _) in splits) {
            storeAll(splitDefinition, inputDiffCollection)
        }

        pt(
                inputDiffCollection,
                RecursiveOrthogonalStateTest::`insert orthogonal state representations`,
                "Insert a state representation split into ${depth + 1} orthogonal states"
        )
    }

    /**
     * Map of the [SplittingOrthogonalStateLineGenerator.SplitResult]s from [splits] to their generated compartments
     */
    private lateinit var compartmentHotSpotDefinitions: Map<
            SplitResult,
            Pair<ElementReference<CompartmentHotSpotDefinition>, ElementReference<CompartmentHotSpotDefinition>>>

    @CouchEditSuiteTest
    @Order(2)
    fun `should have generated a recursive set of CompartmentHotSpotDefinitions`() {

        /*
         * As CompartmentHotSpotDefinitions are designed to show onto which other HotSpotDefinition they depend, we
         * check that for every level of recursive split, two Compartments dependent on the splitting line have been
         * created and that they are also associated with the next higher level of compartment (marking that this
         * compartment is split up by the line into the sub-compartment(s)).
         */

        var previousCompartments: Set<ElementReference<CompartmentHotSpotDefinition>>? = null

        val firstCompartments = mutableMapOf<
                SplitResult,
                Pair<ElementReference<CompartmentHotSpotDefinition>, ElementReference<CompartmentHotSpotDefinition>>>()

        for ((splitDefinition, _, _) in splits) {
            val splittingLine = splitDefinition.line

            val dependentCompartments = testModelRepository.getRelationsToElement(
                    splittingLine.id,
                    CompartmentHotSpotDefinition::class.java,
                    true
            )

            assertThat(dependentCompartments)
                    .describedAs("As each line in the recursive orthogonal state arrangement splits its parent state" +
                            " into two halves, there should be exactly two ComHSDs dependent on each splitting line, " +
                            "got ${dependentCompartments.size} for ${splittingLine.id}")
                    .hasSize(2)

            val expectedIndices = if (splitDefinition.orientation == HORIZONTAL)
                setOf(Pair(0, 0), Pair(0, 1))
            else setOf(Pair(0, 0), Pair(1, 0))

            val actualIndices = dependentCompartments.values.map {
                it.index.indexUL
            }.toSet()

            assertThat(actualIndices).describedAs("For ${splitDefinition.orientation} splits, the expected indices " +
                    "for the ComHSDs dependent on the splitting line are $expectedIndices").isEqualTo(expectedIndices)

            val firstCompartment = dependentCompartments.values.find { it.index.indexUL == Pair(0, 0) }!!
            val secondCompartment = dependentCompartments.values.find {
                it.index.indexUL == Pair(0, 1) || it.index.indexUL == Pair(1, 0)
            }!!

            if (previousCompartments != null) {
                for (compartment in dependentCompartments) {
                    assertThat(compartment.value.splitCompartment)
                            .describedAs("As each line in the recursive orthogonal state arrangement splits its parent state" +
                                    " into two halves, every CompartmentHotSpotDefinition should have its predecessor " +
                                    " as the splitCompartment value")
                            .isIn(previousCompartments)
                }
            }

            previousCompartments = dependentCompartments.values.map { it.ref() }.toSet()

            firstCompartments[splitDefinition] = Pair(firstCompartment.ref(), secondCompartment.ref())
        }

        this.compartmentHotSpotDefinitions = firstCompartments
    }

    private lateinit var compartmentToOrthogonalState: Map<ElementReference<CompartmentHotSpotDefinition>, ElementReference<OrthogonalRegion>>

    @CouchEditSuiteTest
    @Order(3)
    fun `should have generated an OrthogonalState only for each orthogonal state representation that is not further split up`() {
        val newCompartmentToOrthogonalState = mutableMapOf<ElementReference<CompartmentHotSpotDefinition>, ElementReference<OrthogonalRegion>>()

        for ((splitResult, compartments) in compartmentHotSpotDefinitions) {
            val (firstCompartmentHotSpot, secondCompartmentHotSpot) = compartments

            newCompartmentToOrthogonalState[firstCompartmentHotSpot] = assertCompartmentRepresentsOrthogonalState(firstCompartmentHotSpot.id)

            if (splits.last().first === splitResult) {
                newCompartmentToOrthogonalState[secondCompartmentHotSpot] = assertCompartmentRepresentsOrthogonalState(secondCompartmentHotSpot.id)
            } else {
                assertCompartmentDoesNotRepresentOrthogonalState(secondCompartmentHotSpot.id)
            }
        }

        compartmentToOrthogonalState = newCompartmentToOrthogonalState
    }

    @CouchEditSuiteTest
    @Order(4)
    fun `insert a state representation into each area which is not further split up`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for ((_, firstStateRepresentation, secondStateRepresentation) in splits) {
            storeAll(firstStateRepresentation, inputDiffCollection)
            secondStateRepresentation?.let { storeAll(it, inputDiffCollection) }
        }

        pt(
                inputDiffCollection,
                RecursiveOrthogonalStateTest::`insert a state representation into each area which is not further split up`,
                "Insert ${splits.size + 1} states into the orthogonal states"
        )
    }

    @CouchEditSuiteTest
    @Order(5)
    fun `should have inserted a ParentOf relation to each inserted state from the OrthogonalState represented by its compartment`() {
        for ((splitDefinition, stateRepr1, stateRepr2) in splits) {
            val (firstComHSD, secondComHSD) = compartmentHotSpotDefinitions.getValue(splitDefinition)

            assertRepresentedStateParentOfRepresentedRegion(stateRepr1, firstComHSD)
            stateRepr2?.let { assertRepresentedStateParentOfRepresentedRegion(it, secondComHSD) }
        }
    }

    @CouchEditSuiteTest
    @Order(6)
    fun `remove state from first left compartment`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        // remove the state representation contained in the left compartment of the first split
        for (element in splits.first().second.values) {
            inputDiffCollection.mergeCollection(testModelRepository.remove(element.id))
        }

        pt(
                inputDiffCollection,
                RecursiveOrthogonalStateTest::`remove state from first left compartment`,
                "Remove the state from the left compartment of the first split"
        )
    }

    /**
     * Definition for the new split operation to be executed in concrete syntax
     */
    private lateinit var newSplitDefinition: SplitResult

    @CouchEditSuiteTest
    @Order(7)
    fun `add a new splitting line to the first left compartment`() {
        // The area we want to split is the first (left) area in the first split level
        val area = splits.first().first.areas.first

        newSplitDefinition = compartmentLineGenerator.split("newlyInserted", area, HORIZONTAL)

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(newSplitDefinition, inputDiffCollection)

        pt(
                inputDiffCollection,
                RecursiveOrthogonalStateTest::`add a new splitting line to the first left compartment`,
                "Insert a new splitting line to the first left compartment in the concrete syntax"
        )
    }

    private lateinit var subHotSpotDefinitionRefs: List<ElementReference<CompartmentHotSpotDefinition>>

    @CouchEditSuiteTest
    @Order(8)
    fun `should have inserted two new CompartmentHotSpotDefinitions dependent on the first left compartment`() {
        val line = splits.first().first.line

        // get the HotSpotDefinitions that are defined by the first split's line
        val lineDependentHotSpotDefinitions = testModelRepository.getRelationsToElement(
                line.id,
                CompartmentHotSpotDefinition::class.java,
                true
        )

        // get the left of these HotSpotDefinitions. This is the one our new HotSpotDefinitions from step 7 should
        // be children of
        val leftHotSpotDefinition = lineDependentHotSpotDefinitions.values.find { it.index.indexUL == Pair(0, 0) }!!

        val subHotSpotDefinitions = testModelRepository.getRelationsFromElement(
                leftHotSpotDefinition.id,
                CompartmentHotSpotDefinition::class.java,
                true
        )

        assertThat(subHotSpotDefinitions).describedAs("As the Compartment has been split into two in step 7, " +
                "the left compartment of the first split step should have two sub-compartments.").hasSize(2)

        subHotSpotDefinitionRefs = subHotSpotDefinitions.values.map { it.ref() }

        assertThat(subHotSpotDefinitions).allSatisfy { _, v ->
            assertThat(v.lineSet).isEqualTo(setOf(newSplitDefinition.line.ref()))
        }
    }

    @CouchEditSuiteTest
    @Order(9)
    fun `should have inserted an OrthogonalState for each newly created Compartment and deleted the original OrthogonalState for the parent compartment`() {
        for (ref in subHotSpotDefinitionRefs) {
            val representsRelations = testModelRepository.getRelationsFromElement(
                    ref.id,
                    RepresentsOrthogonalState::class.java,
                    true
            )

            assertThat(representsRelations).describedAs(
                    "Each newly inserted Compartment should represent exactly one Orthogonal state."
            ).hasSize(1)
        }

        val (firstCompartment, _) = compartmentHotSpotDefinitions.getValue(splits.first().first)

        assertCompartmentDoesNotRepresentOrthogonalState(firstCompartment.id)
    }

    /**
     * The last (innermost) split operation that has been applied in concrete syntax
     */
    private lateinit var lastSplit: Triple<SplitResult, StateRepresentation, StateRepresentation>

    @CouchEditSuiteTest
    @Order(10)
    fun `move the innermost orthogonal state line out of the parent state`() {
        @Suppress("UNCHECKED_CAST") // the splits List is generated so that the last split gets two StateRepresentations
        lastSplit = splits.last() as Triple<SplitResult, StateRepresentation, StateRepresentation>

        val line = lastSplit.first.line

        // move the line so that it is no longer in the parent state.
        // We don't delete it as deletion is easier to detect and will automatically remove the HotSportDeifinitions,
        val targetX = outerArea.x + outerArea.w + 50

        val points = (line.shape as StraightSegmentLine).points

        val minX = points.map { it.x }.min()!!

        val xDifference = targetX - minX

        for (point in points) {
            point.x += xDifference
        }

        val inputDiffCollection = testModelRepository.store(line)

        pt(
                inputDiffCollection,
                RecursiveOrthogonalStateTest::`move the innermost orthogonal state line out of the parent state`,
                "Move the innermost orthogonal state line to the side of the parent state"
        )
    }

    private lateinit var removedCompartments: List<ElementReference<CompartmentHotSpotDefinition>>

    @CouchEditSuiteTest
    @Order(11)
    fun `should have removed the innermost orthogonal states`() {
        removedCompartments = compartmentHotSpotDefinitions.getValue(lastSplit.first).toList()

        for (compartment in removedCompartments) {
            assertThat(testModelRepository[compartment.id]).describedAs(
                    "The line that the compartment ${compartment.id} was dependent on was moved outside of the " +
                            "orthogonal state's shape. Therefore the compartment should have been deleted").isNull()

            val previousOrthogonalState = compartmentToOrthogonalState[compartment]

            previousOrthogonalState?.let {
                assertThat(testModelRepository[it.id]).describedAs("As the compartment ${compartment.id} has been deleted," +
                        " the orthogonal state ${it.id} previously represented by it should have also been deleted.")
            }
        }
    }

    @CouchEditSuiteTest
    @Order(12)
    fun `should have assigned the states as children of the orthogonal state represented by the next-higher compartment in the hierarchy`() {
        val parentSplitIndex = splits.size - 2

        val expectedParentRef = if (parentSplitIndex < 0) {
            // the state has not been split up more than one time -> we expect the "root" state to be the new parent
            getStateRepresentedBy(outerStateRepresentation)!!
        } else {
            val split = splits[parentSplitIndex].first
            val comHSD = compartmentHotSpotDefinitions.getValue(split).second

            getOrthogonalStateRepresentedBy(comHSD)!!
        }.ref()

        val (_, child1, child2) = lastSplit

        val child1State = getStateRepresentedBy(child1)?.ref()
        // When the number of splits == 1, the "left compartment state" has been removed by step 6, so we can't use that
        child1State?.let { assertParent(it, expectedParentRef) }

        val child2State = getStateRepresentedBy(child2)!!.ref()
        assertParent(child2State, expectedParentRef)
    }

    /**
     * Debug purposes only. Generates strings that enable you to view the graphical situation in JTS TestBuilder.
     * If a test fails, this function can be called from the Debugger to analyze the problem.
     */
    @Suppress("unused")
    private fun dumpToJTS(): Map<String, String> {

        val shapeJTSGeometryProvider = JTSGeometryProvider(PreparedGeometryFactory())

        val serviceCaller = ServiceCaller(testModelRepository)

        val shapeExtractor = injector.getInstance(ShapeExtractor::class.java)

        val rectGeom = shapeJTSGeometryProvider.toGeometry(outerStateRepresentation.outerStateRectangle.shape)

        val lineGeoms = splits.map { shapeJTSGeometryProvider.toGeometry(it.first.line.shape) }

        val hsdGeoms = testModelRepository.getAllIncludingSubTypes(CompartmentHotSpotDefinition::class.java).values.map {
            shapeJTSGeometryProvider.toGeometry(serviceCaller.call(it.ref().asType<ShapedElement<Shape>>(), shapeExtractor::extractShape)!!)
        }

        val geomCollString = fun(input: List<PreparedGeometry>): String = input.joinToString(",", "GEOMETRYCOLLECTION(", ")")

        val lineGeomColl = geomCollString(lineGeoms)

        val hsdGeomColl = geomCollString(hsdGeoms)

        val innerStateReprs = splits.flatMap { (_, stateRepr1, stateRepr2) ->
            val ret = mutableListOf<GraphicObject<*>>()

            ret.add(stateRepr1.outerStateRectangle)
            stateRepr2?.outerStateRectangle?.let(ret::add)

            return@flatMap ret
        }.filter {
            // to avoid confusion, only output those that are currently
            testModelRepository[it.id] != null
        }

        val stateReprGeomColl = geomCollString(innerStateReprs.map { shapeJTSGeometryProvider.toGeometry(it.shape) })

        return mapOf(
                "outer state" to rectGeom.geometry.toString(),
                "lines" to lineGeomColl,
                "HotSpots" to hsdGeomColl,
                "inner states" to stateReprGeomColl
        )
    }
}
