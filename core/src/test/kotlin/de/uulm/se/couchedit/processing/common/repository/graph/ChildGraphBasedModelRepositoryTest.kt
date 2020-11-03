package de.uulm.se.couchedit.processing.common.repository.graph

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryRead
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryReadTest
import de.uulm.se.couchedit.processing.common.testutils.model.*
import de.uulm.se.couchedit.util.extensions.ref
import io.mockk.every
import io.mockk.mockk
import org.jgrapht.graph.DirectedPseudograph
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChildGraphBasedModelRepositoryTest : ModelRepositoryReadTest() {
    private val parentMock: GraphBasedModelRepository = mockk()

    private val graph = DirectedPseudograph<ElementReference<*>, GraphBasedModelRepository.RelationTargetEdge>(
            GraphBasedModelRepository.RelationTargetEdge::class.java
    )

    // Test data for the simulated Parent ModelRepository
    private val elementReferenceMap = mutableMapOf<String, ElementReference<*>>()

    private val insertedElements = mutableMapOf<String, Element>()

    override fun getSystemUnderTestInstance(): ModelRepositoryRead {
        return ChildGraphBasedModelRepository(
                parentMock,
                graph
        )
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()

        every { parentMock.getAll<Element>(any()) } answers {
            ElementQueryResult(insertedElements.filter { it::class.java == firstArg() })
        }

        every { parentMock.getAllIncludingSubTypes<Element>(any()) } answers {
            ElementQueryResult(insertedElements.filter { firstArg<Class<*>>().isInstance(it) })
        }

        every { parentMock.getElementReference(any()) } answers {
            elementReferenceMap[firstArg()]
        }
    }

    @Nested
    inner class RelationGetOperations : ModelRepositoryReadTest.RelationGetOperations() {
        lateinit var element6NotInGraph: Element

        lateinit var relation8NotInGraph: Relation<*, *>
        lateinit var relation9NotInGraph: Relation<*, *>

        @BeforeEach
        override fun insertRelationGraph() {
            super.insertRelationGraph()

            // in addition to the Elements and Relations inserted by super.insertRelationGraph(), we also
            // insert some Elements to the parent repository, but leave them out of the graph for the child repository.
            // This means they should never be included in any get operations.
            element6NotInGraph = givenASimpleElement("f")
            addToParent(element6NotInGraph)

            relation8NotInGraph = givenASimpleTestRelation("08", true, element1.ref(), element6NotInGraph.ref())
            relation9NotInGraph = givenASimpleTestRelation(
                    "09",
                    true,
                    setOf(element5.ref()),
                    setOf(element1.ref(), element6NotInGraph.ref())
            )

            addToParent(relation8NotInGraph)
            addToParent(relation9NotInGraph)
        }

        @Nested
        open inner class GetRelationsFrom : ModelRepositoryReadTest.RelationGetOperations.GetRelationsFrom() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsFromLimitedToClass : ModelRepositoryReadTest.RelationGetOperations.GetRelationsFromLimitedToClass() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsTo : ModelRepositoryReadTest.RelationGetOperations.GetRelationsTo() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsToElement(element1.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsToLimitedToClass : ModelRepositoryReadTest.RelationGetOperations.GetRelationsToLimitedToClass() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsToElement(element1.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsAdjacentTo : ModelRepositoryReadTest.RelationGetOperations.GetRelationsAdjacentTo() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsAdjacentToElement(element1.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsAdjacentToLimitedToClass : ModelRepositoryReadTest.RelationGetOperations.GetRelationsAdjacentToLimitedToClass() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsAdjacentToElement(element1.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsBetween : ModelRepositoryReadTest.RelationGetOperations.GetRelationsBetween() {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsBetweenElements(element5.id, element1.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }

        @Nested
        open inner class GetRelationsBetweenLimitedToClass {
            @Test
            fun `should not return relations that are not in the child graph`() {
                val result = systemUnderTest.getRelationsBetweenElements(element5.id, element1.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation8NotInGraph, relation9NotInGraph)
                )
            }
        }
    }

    override fun givenASimpleTestElementIsInsertedWith(id: String): SimpleTestElement {
        val element = givenASimpleElement(id)

        addToParent(element)
        addToGraph(element)

        return element
    }

    override fun givenASimpleSubclassTestElementIsInsertedWith(id: String): SimpleSubclassTestElement {
        val element = givenASimpleSubclassElement(id)

        addToParent(element)
        addToGraph(element)

        return element
    }

    override fun givenAnOtherSimpleTestElementIsInsertedWith(id: String): OtherSimpleTestElement {
        val element = givenAnOtherSimpleElement(id)

        addToParent(element)
        addToGraph(element)

        return element
    }

    override fun givenASimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleTestRelation {
        val element = givenASimpleTestRelation(id, isDirected, aRef, bRef)

        addToParent(element)
        addToGraph(element)

        return element
    }

    override fun givenASimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRefs: Set<ElementReference<*>>,
            bRefs: Set<ElementReference<*>>
    ): SimpleTestRelation {
        val element = givenASimpleTestRelation(id, isDirected, aRefs, bRefs)

        addToParent(element)
        addToGraph(element)

        return element
    }

    override fun givenASimpleSubclassTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleSubclassTestRelation {
        val element = givenASimpleSubclassRelation(id, isDirected, aRef, bRef)

        addToParent(element)
        addToGraph(element)

        return element
    }

    override fun givenAnOtherSimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): OtherSimpleTestRelation {
        val element = givenAnOtherSimpleRelation(id, isDirected, aRef, bRef)

        addToParent(element)
        addToGraph(element)

        return element
    }

    private fun addToParent(element: Element) {
        val elRef = element.ref()

        elementReferenceMap.putIfAbsent(element.id, elRef)

        every { parentMock[eq(element.id)] } returns element
        every { parentMock[eq(elRef)] } returns element

        val relationTargetEdges = (element as? Relation<*, *>)?.let { createRelationTargetEdges(it) }

        every { parentMock.getRelationTargetEdges(eq(element.id)) } returns relationTargetEdges

        insertedElements[element.id] = element
    }

    private fun addToGraph(element: Element) {
        graph.addVertex(element.ref())

        if (element is Relation<*, *>) {
            val edges = createRelationTargetEdges(element)

            for (edge in edges) {
                graph.addEdge(
                        edge.fromRef,
                        edge.toRef,
                        edge
                )
            }
        }
    }

    private fun createRelationTargetEdges(rel: Relation<*, *>): Set<GraphBasedModelRepository.RelationTargetEdge> {
        val ret = mutableSetOf<GraphBasedModelRepository.RelationTargetEdge>()

        for (ref in rel.aSet) {
            ret.add(GraphBasedModelRepository.RelationTargetEdge(
                    fromRef = ref,
                    toRef = rel.ref(),
                    relationInfo = rel.ref(),
                    isReverse = false,
                    type = GraphBasedModelRepository.RelationTargetEdge.Type.A
            ))

            if (!rel.isDirected) {
                ret.add(GraphBasedModelRepository.RelationTargetEdge(
                        fromRef = rel.ref(),
                        toRef = ref,
                        relationInfo = rel.ref(),
                        isReverse = true,
                        type = GraphBasedModelRepository.RelationTargetEdge.Type.A
                ))
            }
        }

        for (ref in rel.bSet) {
            ret.add(GraphBasedModelRepository.RelationTargetEdge(
                    fromRef = rel.ref(),
                    toRef = ref,
                    relationInfo = rel.ref(),
                    isReverse = false,
                    type = GraphBasedModelRepository.RelationTargetEdge.Type.B
            ))

            if (!rel.isDirected) {
                ret.add(GraphBasedModelRepository.RelationTargetEdge(
                        fromRef = ref,
                        toRef = rel.ref(),
                        relationInfo = rel.ref(),
                        isReverse = true,
                        type = GraphBasedModelRepository.RelationTargetEdge.Type.B
                ))
            }
        }

        return ret
    }
}
