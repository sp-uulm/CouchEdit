package de.uulm.se.couchedit.statecharts.scenarios.compartment.grid

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Point
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
import de.uulm.se.couchedit.statecharts.scenarios.BaseStatechartScenario
import de.uulm.se.couchedit.statecharts.testdata.OrthogonalStateGridGenerator
import de.uulm.se.couchedit.statecharts.testdata.OrthogonalStateLineRepresentation
import de.uulm.se.couchedit.statecharts.testdata.SplittingOrthogonalStateLineGenerator
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator
import de.uulm.se.couchedit.statecharts.testmodel.OrthogonalStateGridTestDetails
import de.uulm.se.couchedit.systemtestutils.test.SystemTestProcessor
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order
import org.locationtech.jts.geom.prep.PreparedGeometry
import org.locationtech.jts.geom.prep.PreparedGeometryFactory

/**
 * Scenario for testing the generation of "peer" compartments (i.e. no compartment is dependent on another one)
 * and their translation into orthogonal states (Regions).
 * This is in contrast to the RecursiveOrthogonalStateTest where the Compartments are generated to be dependent on each
 * other.
 *
 * The following steps are executed:
 * 1. Insert a state representation of size [outermostWidth] x [outermostHeight] as given by the [stateRepresentationGenerator]
 *    along with a set of vertical and horizontal lines generated by the [compartmentGridGenerator]. This is done
 *    in one DiffCollection.
 *
 * 2. Assert that in the same fashion the orthogonal regions were drawn, the processing has produced a grid of
 *    [CompartmentHotSpotDefinitions] which are all
 *    * dependent on the entire set of lines inserted
 *    * Not dependent on each other
 *
 * 3. Assert that for every one of these HotSpotDefinitions, an [OrthogonalState] has been created.
 *
 * 4. For all areas created by the orthogonal state grid, insert a representation for a "child state".
 *    All of these "child state representations" are inserted as a single DiffCollection.
 *
 * 5. Assert that all of these "child state" representations have been assigned a [State] Abstract Syntax element and
 *    that this [State] Element has got a [ParentOf] relation from the [OrthogonalRegion] which is represented by the
 *    Area it has been inserted in.
 *
 *    This serves two purposes: Checking whether the Compartment mechanism correctly generates the geometries of "peer"
 *    Compartments and whether Concrete-Syntax Contains relations from the Core mechanism are correctly translated
 *    into Abstract-Syntax ParentOf relations for the represented Elements.
 *
 * 6. Remove the bottom right "child" state from the grid again. This is to prepare for step 7.
 *
 * 7. Insert a horizontal splitting line in the bottom right orthogonal state, similar to the steps of the
 *    RecursiveOrthogonalStateTest.
 *
 * 8. Assert that the original [OrthogonalRegion] represented by the bottom right split area has been removed, instead
 *    two new [OrthogonalRegion]s were inserted which are represented by the new compartments
 *
 * 9. Into the designated areas of the new dependent [CompartmentHotSpotDefinition]s, insert two new State
 *    representations.
 *
 * 10. Assert that both of these State Representations have been assigned a [State] Abstract-Syntax Element and that
 *     these [State] Elements have a [ParentOf] Relation from the OrthogonalRegion (from step 8)
 *     of which the representation contains the respective State Representation
 *
 * 11. Move the first horizontal line aside, to the right of the parent State Representation.
 *     This merges the first two rows of Orthogonal State representations in the Concrete Syntax.
 *     As with the RecursiveOrthogonalStateTest, we don't delete the line as deletion is automatically detected via the
 *     contract of the ModelRepository.
 *
 *  12. Assert that the compartments with the biggest index in Y direction have been removed (because through the
 *      removal of the line, the total number of Compartment rows is now one less than before. Compartments are never
 *      deleted in the middle, thus the bottommost Compartments get removed and the grid is rearranged.
 *
 *  13. Assert that for each previously inserted child state in the grid (save for the deleted bottommost right one)
 *      the parent of the state in the Abstract Syntax is now the one represented by the [CompartmentHotSpotDefinition]
 *      one row higher up.
 *
 *      The [State]s represented by the first row of GraphicObjects should stay children of the
 *      [OrthogonalRegion]s represented by the (x, 0) row of [CompartmentHotSpotDefinition]s.
 *
 *  14. Assert that the sub-compartments from Step 7 are still intact after the removal of the line - i.e. the
 *      [splitResult]'s line has still [CompartmentHotSpotDefinition]s depending on it and the states
 *      in the [insertedStateRepresentations] still have the correct one of these HotSpotDefinitions as their parent.
 */
class OrthogonalStateGridTest(
        private val gridSizeX: Int,
        private val gridSizeY: Int,
        private val outermostWidth: Double,
        private val outermostHeight: Double
) : BaseStatechartScenario() {
    override val testInstanceInfo = TestInstanceInfo(
            "OSGT",
            "OrthogonalStateGridTest $gridSizeX x $gridSizeY",
            OrthogonalStateGridTestDetails(gridSizeX, gridSizeY, gridSizeX * gridSizeY)
    )

    private val roundedEdgeSize: Double = 10.0

    /**
     * Distance that all compartment-splitting lines should have from the outer border of their Area.
     */
    private val compartmentLineMargin: Double = 3.0

    /**
     * Margin that the inner states will get in their respective parent orthogonal state.
     */
    private val stateMargin: Double = 5.0

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
        GridAreasGenerator.Area(0.0, 0.0, outermostWidth, outermostHeight)
    }

    private val outerStateRepresentation by disposableLazy {
        stateRepresentationGenerator.getStateRepresentationFrom(outerArea, "outer", StateRepresentationGenerator.LabelPosition.Center)
    }

    private val compartmentGridGenerator by disposableLazy {
        OrthogonalStateGridGenerator(gridSizeX, gridSizeY, compartmentLineMargin)
    }

    private val splittingLineGenerator by disposableLazy {
        SplittingOrthogonalStateLineGenerator(compartmentLineMargin)
    }

    private val gridDefinition by disposableLazy {
        compartmentGridGenerator.generateCompartmentGrid(outerArea)
    }

    @CouchEditSuiteTest
    @Order(1)
    fun `insert orthogonal state representations`() {
        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(outerStateRepresentation, inputDiffCollection)

        for (lineRepr in gridDefinition.allLines) {
            storeAll(lineRepr, inputDiffCollection)
        }

        pt(
                inputDiffCollection,
                OrthogonalStateGridTest::`insert orthogonal state representations`,
                "Insert State with $gridSizeX x $gridSizeY orthogonal states"
        )
    }

    lateinit var gridCompartments: Map<Pair<Int, Int>, ElementReference<CompartmentHotSpotDefinition>>

    @CouchEditSuiteTest
    @Order(2)
    fun `should have generated a set of CompartmentHotSpotDefinitions on the same level`() {
        val comHSDs = testModelRepository.getRelationsFromElement(
                outerStateRepresentation.outerStateRectangle.id,
                CompartmentHotSpotDefinition::class.java,
                true
        )

        assertThat(comHSDs).allSatisfy { _, v ->
            assertThat(v.splitCompartment).describedAs(
                    "None of the compartments generated by the grid should depend on other compartments"
            ).isNull()
            assertThat(v.lineSet).describedAs(
                    "All of the compartments generated by the grid should reference all lines of the grid"
            ).containsExactlyInAnyOrderElementsOf(gridDefinition.allLines.map { it.line.ref() })
        }

        /**
         * the indices that should be present in the generated CompartmentHotSpotDefinitions according to the grid
         * generated by the compartmentGridGenerator.
         */
        val gridDefinitionIndexes = gridDefinition.grid.cellSet().map {
            return@map Pair(it.columnKey!!, it.rowKey!!)
        }

        /**
         * The indices that the HotSpotDefinitions that were created by the processing actually have
         */
        val hotSpotDefinitionIndexes = comHSDs.values.map {
            it.index.indexUL
        }

        assertThat(hotSpotDefinitionIndexes).describedAs(
                "The indices of the HotSpotDefinitions generated must be equal to the indices of the original " +
                        "Orthogonal State Grid"
        ).containsExactlyInAnyOrderElementsOf(gridDefinitionIndexes)

        gridCompartments = comHSDs.mapKeys { (_, hsd) -> hsd.index.indexUL }.mapValues { (_, hsd) -> hsd.ref() }
    }

    @CouchEditSuiteTest
    @Order(3)
    fun `should have generated an OrthogonalState for each HotSpotDefinition`() {
        for (comHSD in gridCompartments.values) {
            val orthogonalState = getOrthogonalStateRepresentedBy(comHSD)

            assertThat(orthogonalState)
                    .describedAs("Every CompartmentHotSpotDefinition in the grid should get an OrthogonalState")
                    .isNotNull()
        }
    }

    lateinit var childStateRepresentations: Map<Pair<Int, Int>, StateRepresentationGenerator.StateRepresentation>

    @CouchEditSuiteTest
    @Order(4)
    fun `insert a child state into each Area`() {
        childStateRepresentations = gridDefinition.grid.cellSet().map {
            (it.columnKey!! to it.rowKey!!) to stateRepresentationGenerator.getStateRepresentationFrom(
                    GridAreasGenerator.reduceAreaByMargin(it.value!!, stateMargin),
                    "(${it.columnKey},${it.rowKey})"
            )
        }.toMap()

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for (representation in childStateRepresentations.values) {
            storeAll(representation, inputDiffCollection)
        }

        pt(
                inputDiffCollection,
                OrthogonalStateGridTest::`insert a child state into each Area`,
                "Insert ${inputDiffCollection.size} state representation into the grid of OrthogonalStates"
        )
    }

    @CouchEditSuiteTest
    @Order(5)
    fun `should have inserted a ParentOf relation from each Orthogonal State to its contained state`() {
        for ((index, stateRepr) in childStateRepresentations) {
            val orthogonalRegion = gridCompartments.getValue(index)

            assertRepresentedStateParentOfRepresentedRegion(
                    stateRepr,
                    orthogonalRegion
            )
        }
    }

    @CouchEditSuiteTest
    @Order(6)
    fun `remove bottom right State from the grid`() {
        val bottomRightStateRepresentation = childStateRepresentations.getValue(Pair(gridSizeX - 1, gridSizeY - 1))

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        for ((_, value) in bottomRightStateRepresentation) {
            inputDiffCollection.mergeCollection(testModelRepository.remove(value.id))
        }

        pt(
                inputDiffCollection,
                OrthogonalStateGridTest::`remove bottom right State from the grid`,
                "Remove the bottom right state from the Element grid"
        )
    }

    lateinit var splitResult: SplittingOrthogonalStateLineGenerator.SplitResult

    @CouchEditSuiteTest
    @Order(7)
    fun `split the bottom right orthogonal state into two sub-states`() {
        val bottomRightArea = gridDefinition.grid.get(gridSizeY - 1, gridSizeX - 1)

        splitResult = splittingLineGenerator.split(
                "sub_split",
                bottomRightArea,
                OrthogonalStateLineRepresentation.LineOrientation.HORIZONTAL
        )

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(splitResult.lineRepresentation, inputDiffCollection)

        pt(
                inputDiffCollection,
                OrthogonalStateGridTest::`split the bottom right orthogonal state into two sub-states`,
                "Split the bottom right orthogonal state in two sub-states"
        )
    }

    /**
     * Mapping of the indices of the [CompartmentHotSpotDefinition]s (= also the area indices of [splitResult]'s
     * [SplittingOrthogonalStateLineGenerator.SplitResult.areas]) generated in step 7 to [ElementReference]s of the
     * [OrthogonalRegion]s which are represented by these Compartments in the abstract syntax.
     */
    private lateinit var subCompartmentIndexToOrthogonalState: Map<Pair<Int, Int>, ElementReference<OrthogonalRegion>>

    @CouchEditSuiteTest
    @Order(8)
    fun `should have removed the OrthogonalState for the original parent compartment and inserted two new ones`() {
        val compartment = gridCompartments.getValue(Pair(gridSizeX - 1, gridSizeY - 1))

        assertCompartmentDoesNotRepresentOrthogonalState(compartment.id)

        // get the compartments that are dependent on the newly inserted line -> those are the ones representing a
        // new OrthogonalState
        val lineId = splitResult.line.id

        val newCompartments = testModelRepository.getRelationsToElement(
                splitResult.line.id,
                CompartmentHotSpotDefinition::class.java,
                true
        )

        assertThat(newCompartments).describedAs("It is expected that two new compartments have been created " +
                "by line $lineId").hasSize(2)

        val newCompartmentIndexToOrthogonalState = mutableMapOf<Pair<Int, Int>, ElementReference<OrthogonalRegion>>()

        for ((id, newCompartment) in newCompartments) {
            newCompartmentIndexToOrthogonalState[newCompartment.index.indexUL] = assertCompartmentRepresentsOrthogonalState(id)
        }

        subCompartmentIndexToOrthogonalState = newCompartmentIndexToOrthogonalState.toMap()
    }

    /**
     * Concrete Syntax state representations inserted in Step 9.
     *
     * The key is the index of the Area / Compartment among the split results from [subCompartmentIndexToOrthogonalState],
     * the value is the assigned [StateRepresentationGenerator.StateRepresentation].
     */
    private lateinit var insertedStateRepresentations: Map<Pair<Int, Int>, StateRepresentationGenerator.StateRepresentation>

    @CouchEditSuiteTest
    @Order(9)
    fun `insert a new state representation in both of the new orthogonal regions`() {
        val (area1, area2) = splitResult.areas

        val index1 = Pair(0, 0)
        val index2 = Pair(0, 1)

        val newInsertedStateRepresentations = mutableMapOf<Pair<Int, Int>, StateRepresentationGenerator.StateRepresentation>()

        val stateRepr1 = stateRepresentationGenerator.getStateRepresentationFrom(
                GridAreasGenerator.reduceAreaByMargin(area1, stateMargin),
                "sub_split_state_1"
        )
        val stateRepr2 = stateRepresentationGenerator.getStateRepresentationFrom(
                GridAreasGenerator.reduceAreaByMargin(area2, stateMargin),
                "sub_split_state_2"
        )

        val inputDiffCollection = testDiffCollectionFactory.createMutableTimedDiffCollection()

        storeAll(stateRepr1, inputDiffCollection)
        storeAll(stateRepr2, inputDiffCollection)

        newInsertedStateRepresentations[index1] = stateRepr1
        newInsertedStateRepresentations[index2] = stateRepr2

        pt(
                inputDiffCollection,
                OrthogonalStateGridTest::`insert a new state representation in both of the new orthogonal regions`,
                "Insert two new state representations into the new orthogonal regions"
        )

        insertedStateRepresentations = newInsertedStateRepresentations
    }

    @CouchEditSuiteTest
    @Order(10)
    fun `should have inserted a ParentOf relation towards each of the newly created states from its parent region`() {
        for ((index, stateRepresentation) in insertedStateRepresentations) {
            val compartment = subCompartmentIndexToOrthogonalState.getValue(index)

            val state = getStateRepresentedBy(stateRepresentation)!!.ref()

            assertParent(state, compartment)
        }
    }

    @CouchEditSuiteTest
    @Order(11)
    fun `move the topmost horizontal line out of the parent state representation's area`() {
        val lineReprToMove = gridDefinition.horizontalLines.first()

        val points = (lineReprToMove.line.shape as StraightSegmentLine).points

        val targetX = outerArea.x + outerArea.w + stateMargin

        val minX = points.map(Point::x).min()!!

        val xDiff = targetX - minX

        for (point in points) {
            point.x += xDiff
        }

        val inputDiffCollection = testModelRepository.store(lineReprToMove.line)

        pt(
                inputDiffCollection,
                OrthogonalStateGridTest::`move the topmost horizontal line out of the parent state representation's area`,
                "Move the topmost line out of the parent state representation"
        )
    }

    @CouchEditSuiteTest
    @Order(12)
    fun `should have removed the Compartments with the biggest y-index`() {
        for (i in 0 until gridSizeX) {
            val compartmentIndex = Pair(i, gridSizeY - 1)

            val comHSDRef = gridCompartments.getValue(compartmentIndex)

            val comHSDValue = testModelRepository[comHSDRef]

            assertThat(comHSDValue).describedAs("As the topmost horizontal line was removed, the grid size should " +
                    "now be one less. Therefore, the Compartment with index=$compartmentIndex should have been deleted.")
                    .isNull()
        }
    }

    @CouchEditSuiteTest
    @Order(13)
    fun `should have moved all child states but the first row one row higher up in the grid`() {
        /*
         * fetch the HotSpotDefinitions that are currently known in the parent state representation and not dependent on
         * another Compartment (i.e. no sub-compartments)
         */

        val newHotSpotDefinitions = testModelRepository.getRelationsFromElement(
                outerStateRepresentation.outerStateRectangle.id,
                CompartmentHotSpotDefinition::class.java,
                true
        ).filter { (_, hsd) -> hsd.splitCompartment == null }
                .mapKeys { (_, hsd) -> hsd.index.indexUL }

        for (i in 0 until gridSizeX) {
            /*
             * assert that each State represented by an Element of the 0-th row of State representations is still a
             * child of the OrthogonalRegion represented by the corresponding 0-th row Compartment
             */
            val firstRowCompartment = newHotSpotDefinitions.getValue(Pair(i, 0)).ref()
            val firstRowChildStateRepresentation = childStateRepresentations.getValue(Pair(i, 0))

            if (getOrthogonalStateRepresentedBy(firstRowCompartment) != null) {
                assertRepresentedStateParentOfRepresentedRegion(firstRowChildStateRepresentation, firstRowCompartment)
            }

            for (j in 1 until gridSizeY) {
                val compartmentHotSpotDefinitionRef = newHotSpotDefinitions.getValue(Pair(i, j - 1)).ref()
                val childStateRepresentation = childStateRepresentations.getValue(Pair(i, j))

                // skip State Representations that we have deleted in prior steps!
                if (testModelRepository[childStateRepresentation.outerStateRectangle.id] == null) {
                    continue
                }

                assertRepresentedStateParentOfRepresentedRegion(
                        childStateRepresentation,
                        compartmentHotSpotDefinitionRef
                )
            }
        }
    }

    @CouchEditSuiteTest
    @Order(14)
    fun `should have retained the sub-compartments of the bottom-right orthogonal state and their OrthogonalStates`() {
        val comHSDs = testModelRepository.getRelationsToElement(
                splitResult.line.id,
                CompartmentHotSpotDefinition::class.java,
                true
        )

        assertThat(comHSDs).describedAs(
                "The set of CompartmentHotSpotDefinitions referencing the sub-compartment splitting line " +
                        "${splitResult.line.id} should still contain two Elements"
        ).hasSize(2)

        assertThat(comHSDs.map { it.value.index.indexUL }).describedAs(
                "The set of CompartmentHotSpotDefinitions referencing the sub-compartment splitting line " +
                        "${splitResult.line.id} should still contain the indices (0,0) and (0,1)"
        ).containsExactlyInAnyOrder(Pair(0, 0), Pair(0, 1))

        for ((_, comHSD) in comHSDs) {
            assertRepresentedStateParentOfRepresentedRegion(
                    insertedStateRepresentations.getValue(comHSD.index.indexUL),
                    comHSD.ref()
            )
        }
    }

    /**
     * Debug purposes only. Generates strings that enable you to view the graphical situation in JTS TestBuilder.
     * If a test fails, this function can be called from the Debugger to analyze the problem.
     */
    @Suppress("unused")
    fun dumpToJTS(): Map<String, String> {

        val shapeJTSGeometryProvider = JTSGeometryProvider(PreparedGeometryFactory())

        val serviceCaller = ServiceCaller(testModelRepository)

        val shapeExtractor = injector.getInstance(ShapeExtractor::class.java)

        val rectGeom = shapeJTSGeometryProvider.toGeometry(outerStateRepresentation.outerStateRectangle.shape)

        val lineGeoms = gridDefinition.allLines.map { shapeJTSGeometryProvider.toGeometry(it.line.shape) }

        val hsdGeoms = testModelRepository.getAllIncludingSubTypes(CompartmentHotSpotDefinition::class.java).values.map {
            val shape = serviceCaller.call(it.ref().asType<ShapedElement<Shape>>(), shapeExtractor::extractShape)

            shapeJTSGeometryProvider.toGeometry(shape!!)
        }

        val geomCollString = fun(input: List<PreparedGeometry>): String = input.joinToString(",", "GEOMETRYCOLLECTION(", ")")

        val lineGeomColl = geomCollString(lineGeoms)

        val hsdGeomColl = geomCollString(hsdGeoms)

        return mapOf(
                "outer state" to rectGeom.geometry.toString(),
                "lines" to lineGeomColl,
                "HotSpots" to hsdGeomColl
                //"inner states" to stateReprGeomColl
        )
    }
}
