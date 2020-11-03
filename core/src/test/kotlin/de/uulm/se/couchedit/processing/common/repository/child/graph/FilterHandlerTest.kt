package de.uulm.se.couchedit.processing.common.repository.child.graph

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.repository.child.specification.Filter
import de.uulm.se.couchedit.processing.common.repository.graph.GraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.testutils.model.*
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jgrapht.graph.AbstractGraph
import org.jgrapht.graph.Pseudograph
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class FilterHandlerTest {
    protected val modelRepositoryMock: RootGraphBasedModelRepository = mockk()

    protected lateinit var graph: Pseudograph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>

    protected val eRef1 = ElementReference("01", SimpleTestElement::class.java)
    protected val eRef2 = ElementReference("02", SimpleSubclassTestElement::class.java)
    protected val eRef3 = ElementReference("03", SimpleTestElement::class.java)
    protected val eRef4 = ElementReference("04", OtherSimpleTestElement::class.java)
    protected val eRef5 = ElementReference("05", OtherSimpleTestElement::class.java)

    protected val rRefA = ElementReference("a", SimpleTestRelation::class.java)
    protected val rRefB = ElementReference("b", SimpleSubclassTestRelation::class.java)
    protected val rRefC = ElementReference("c", OtherSimpleTestRelation::class.java)
    protected val rRefD = ElementReference("d", OtherSimpleTestRelation::class.java)

    protected val edge1 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = eRef1,
            toRef = rRefA,
            relationInfo = rRefA,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.A,
            isReverse = false
    )

    protected val edge2 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = rRefA,
            toRef = eRef2,
            relationInfo = rRefA,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.B,
            isReverse = false
    )

    protected val edge3 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = eRef1,
            toRef = rRefB,
            relationInfo = rRefB,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.A,
            isReverse = false
    )

    protected val edge4 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = rRefB,
            toRef = eRef2,
            relationInfo = rRefB,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.B,
            isReverse = false
    )

    protected val edge5 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = rRefB,
            toRef = eRef4,
            relationInfo = rRefB,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.B,
            isReverse = false
    )

    protected val edge6 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = eRef3,
            toRef = rRefC,
            relationInfo = rRefC,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.A,
            isReverse = false
    )

    protected val edge7 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = eRef4,
            toRef = rRefC,
            relationInfo = rRefC,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.A,
            isReverse = false
    )

    protected val edge8 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = rRefC,
            toRef = eRef5,
            relationInfo = rRefC,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.B,
            isReverse = false
    )

    protected val edge9 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = eRef4,
            toRef = rRefD,
            relationInfo = rRefD,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.A,
            isReverse = false
    )

    protected val edge10 = GraphBasedModelRepository.RelationTargetEdge(
            fromRef = rRefD,
            toRef = eRef5,
            relationInfo = rRefD,
            type = GraphBasedModelRepository.RelationTargetEdge.Type.B,
            isReverse = false
    )

    @BeforeEach
    fun setUp() {
        // build a graph
        this.graph = Pseudograph(GraphBasedModelRepository.RelationTargetEdge::class.java)

        listOf(eRef1, eRef2, eRef3, eRef4, eRef5, rRefA, rRefB, rRefC, rRefD).forEach {
            this.graph.addVertex(it)
        }

        listOf(edge1, edge2, edge3, edge4, edge5, edge6, edge7, edge8, edge9, edge10).forEach {
            this.graph.addEdge(it.fromRef, it.toRef, it)
        }
    }

    @Nested
    inner class ElementType {
        @Test
        fun `should return a graph with only the given types if INCLUDEONLY mode is specified`() {
            val filter = Filter.ElementType(Filter.Mode.INCLUDEONLY, setOf(
                    SimpleTestElement::class.java,
                    SimpleTestRelation::class.java
            ))


            FilterHandler.handle(modelRepositoryMock, graph, filter)

            // Graph must also not contain Relation B as one of its endpoints is a OtherSimpleTestElement.
            assertGraphContains(
                    graph = graph,
                    mustBeContainedVertices = setOf(eRef1, eRef2, eRef3, rRefA),
                    mustNotBeContainedVertices = setOf(eRef4, eRef5, rRefB, rRefC, rRefD),
                    mustBeContainedEdges = setOf(edge1, edge2),
                    mustNotBeContainedEdges = setOf(edge3, edge4, edge5, edge6, edge7, edge8, edge9, edge10)
            )
        }

        @Test
        fun `should return a graph without the given types if EXCLUDE mode is specified`() {
            val filter = Filter.ElementType(Filter.Mode.EXCLUDE, setOf(
                    SimpleTestElement::class.java,
                    SimpleTestRelation::class.java
            ))


            FilterHandler.handle(modelRepositoryMock, graph, filter)

            // Graph must also not contain Relation B as one of its endpoints is a OtherSimpleTestElement.
            assertGraphContains(
                    graph = graph,
                    mustBeContainedVertices = setOf(eRef4, eRef5, rRefD),
                    mustNotBeContainedVertices = setOf(eRef1, eRef2, eRef3, rRefA, rRefB, rRefC),
                    mustBeContainedEdges = setOf(edge9, edge10),
                    mustNotBeContainedEdges = setOf(edge1, edge2, edge3, edge4, edge5, edge6, edge7, edge8)
            )
        }
    }

    @Nested
    inner class RelationsWithEndpoints {
        @Test
        fun `should return only Relations with any of the specified types as their endpoint if INCLUDEONLY is specified`() {
            val filter = Filter.RelationsWithEndpoints(
                    mode = Filter.Mode.INCLUDEONLY,
                    relationType = OtherSimpleTestRelation::class.java,
                    aTypes = setOf(OtherSimpleTestElement::class.java)
            )


            FilterHandler.handle(modelRepositoryMock, graph, filter)


            // Graph must also not contain Relation B as one of its endpoints is a OtherSimpleTestElement.
            assertGraphContains(
                    graph = graph,
                    mustBeContainedVertices = setOf(eRef1, eRef2, eRef3, eRef4, eRef5, rRefA, rRefB, rRefD),
                    mustNotBeContainedVertices = setOf(rRefC),
                    mustBeContainedEdges = setOf(edge1, edge2, edge3, edge4, edge5, edge9, edge10),
                    mustNotBeContainedEdges = setOf(edge6, edge7, edge8)
            )
        }

        @Test
        fun `should return only Relations with any of the specified types as their endpoint if EXCLUDE is specified`() {
            val filter = Filter.RelationsWithEndpoints(
                    mode = Filter.Mode.EXCLUDE,
                    relationType = SimpleTestRelation::class.java,
                    bTypes = setOf(OtherSimpleTestElement::class.java)
            )


            FilterHandler.handle(modelRepositoryMock, graph, filter)


            // Graph must also not contain Relation B as one of its endpoints is a OtherSimpleTestElement.
            assertGraphContains(
                    graph = graph,
                    mustBeContainedVertices = setOf(eRef1, eRef2, eRef3, eRef4, eRef5, rRefA, rRefC, rRefD),
                    mustNotBeContainedVertices = setOf(rRefB),
                    mustBeContainedEdges = setOf(edge1, edge2, edge6, edge7, edge8, edge9, edge10),
                    mustNotBeContainedEdges = setOf(edge3, edge4, edge5)
            )
        }
    }

    private fun assertGraphContains(
            graph: AbstractGraph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>,
            mustBeContainedVertices: Set<ElementReference<*>>,
            mustBeContainedEdges: Set<GraphBasedModelRepository.RelationTargetEdge>,
            mustNotBeContainedVertices: Set<ElementReference<*>>,
            mustNotBeContainedEdges: Set<GraphBasedModelRepository.RelationTargetEdge>
    ) {
        for (vertex in mustBeContainedVertices) {
            assertThat(graph.containsVertex(vertex)).describedAs("Graph must contain $vertex").isTrue()
        }

        for (vertex in mustNotBeContainedVertices) {
            assertThat(graph.containsVertex(vertex)).describedAs("Graph must not contain $vertex").isFalse()
        }

        for (edge in mustBeContainedEdges) {
            assertThat(graph.containsEdge(edge)).describedAs("Graph must contain edge " +
                    "${edge.relationInfo} / Type ${edge.type}").isTrue()
        }

        for (edge in mustNotBeContainedEdges) {
            assertThat(graph.containsEdge(edge)).describedAs("Graph must not contain edge " +
                    "${edge.relationInfo} / Type ${edge.type}").isFalse()
            assertThat(graph.containsVertex(edge.relationInfo)).describedAs("Graph must not contain relation vertex" +
                    "${edge.relationInfo} as it may also not contain the edges for this Relation").isFalse()
        }
    }
}
