package de.uulm.se.couchedit.processing.common.repository

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.testutils.model.*
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("unused")
abstract class ModelRepositoryReadTest {
    protected abstract fun getSystemUnderTestInstance(): ModelRepositoryRead

    protected open lateinit var systemUnderTest: ModelRepositoryRead

    @BeforeEach
    protected open fun setUp() {
        this.systemUnderTest = getSystemUnderTestInstance()
    }

    /**
     * Collection of Tests for getRelations... operations.
     *
     * Collected within this nested test class for common set-up.
     */
    @Nested
    open inner class RelationGetOperations {
        lateinit var element1: Element
        lateinit var element2: Element
        lateinit var element3: Element
        lateinit var element4: Element
        lateinit var element5: Element

        lateinit var relation1: Relation<*, *>
        lateinit var relation2: Relation<*, *>
        lateinit var relation3: Relation<*, *>
        lateinit var relation4: Relation<*, *>
        lateinit var relation5: Relation<*, *>
        lateinit var relation6: Relation<*, *>
        lateinit var relation7: Relation<*, *>

        @BeforeEach
        open fun insertRelationGraph() {
            element1 = givenASimpleTestElementIsInsertedWith("a")
            element2 = givenASimpleSubclassTestElementIsInsertedWith("b")
            element3 = givenAnOtherSimpleTestElementIsInsertedWith("c")
            element4 = givenAnOtherSimpleTestElementIsInsertedWith("d")
            element5 = givenAnOtherSimpleTestElementIsInsertedWith("e")

            relation1 = givenASimpleTestRelationIsInsertedWith("01", true, element1.ref(), element2.ref())
            relation2 = givenASimpleSubclassTestRelationIsInsertedWith("02", false, element1.ref(), element2.ref())
            relation3 = givenAnOtherSimpleTestRelationIsInsertedWith("03", true, element1.ref(), element3.ref())
            relation4 = givenASimpleSubclassTestRelationIsInsertedWith("04", true, element2.ref(), element3.ref())
            relation5 = givenASimpleSubclassTestRelationIsInsertedWith("05", true, element2.ref(), element1.ref())
            relation6 = givenASimpleTestRelationIsInsertedWith(
                    "06",
                    true,
                    setOf(element1.ref(), element4.ref()),
                    setOf(element2.ref(), element3.ref())
            )
            relation7 = givenASimpleTestRelationIsInsertedWith("07", true, element4.ref(), element5.ref())
        }

        @Nested
        open inner class GetRelationsFrom {
            @Test
            open fun `should return all Relations that originate from the given Element ID`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation3, relation6)
                )
            }

            @Test
            open fun `should not return relations that are only incoming to the element`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation4)
                )
            }

            @Test
            open fun `should not return relations that are not adjacent to the element`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation5, relation7)
                )
            }

            @Test
            open fun `should return an empty result if the ID is not in the Repository`() {
                val result = systemUnderTest.getRelationsFromElement("bogus")

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsFromLimitedToClass {
            @Test
            open fun `should return all Relations of the given class that originate from the given Element ID`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id, SimpleTestRelation::class.java)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation6),
                        mustNotBeContained = setOf(relation2, relation3, relation4, relation5, relation7)
                )
            }

            @Test
            open fun `should return all Relations of the given class and its subclasses that originate from the given Element ID if includeSubClasses is set`() {
                val result = systemUnderTest.getRelationsFromElement(element1.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation6),
                        mustNotBeContained = setOf(relation3, relation4, relation5, relation7)
                )
            }

            @Test
            open fun `should return an empty result if the ID is not in the Repository`() {
                val result = systemUnderTest.getRelationsFromElement("bogus", SimpleTestRelation::class.java, true)

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsTo {
            @Test
            open fun `should return all Relations that go to the given Element ID`() {
                val result = systemUnderTest.getRelationsToElement(element2.id)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation6)
                )
            }

            @Test
            open fun `should not return relations that are only going out from the element`() {
                val result = systemUnderTest.getRelationsToElement(element2.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation4, relation5)
                )
            }

            @Test
            open fun `should not return relations that are not adjacent to the element`() {
                val result = systemUnderTest.getRelationsToElement(element2.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation3, relation7)
                )
            }

            @Test
            open fun `should return an empty result if the ID is not in the Repository`() {
                val result = systemUnderTest.getRelationsToElement("bogus")

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsToLimitedToClass {
            @Test
            open fun `should return all Relations of the given class that go to the given Element ID`() {
                val result = systemUnderTest.getRelationsToElement(element2.id, SimpleTestRelation::class.java)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation6),
                        mustNotBeContained = setOf(relation2, relation3, relation4, relation5)
                )
            }

            @Test
            open fun `should return all Relations of the given class and its subclasses that go to the given Element ID if includeSubClasses is set`() {
                val result = systemUnderTest.getRelationsToElement(element2.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation6),
                        mustNotBeContained = setOf(relation4, relation5)
                )
            }

            @Test
            open fun `should return an empty result if the ID is not in the Repository`() {
                val result = systemUnderTest.getRelationsToElement("bogus", SimpleTestRelation::class.java, true)

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsAdjacentTo {
            @Test
            open fun `should return all Relations that originate from or go to the given Element ID`() {
                val result = systemUnderTest.getRelationsAdjacentToElement(element2.id)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation4, relation5, relation6)
                )
            }

            @Test
            open fun `should not return relations that are not adjacent to the element`() {
                val result = systemUnderTest.getRelationsAdjacentToElement(element2.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation3, relation7)
                )
            }

            @Test
            open fun `should return an empty result if the ID is not in the Repository`() {
                val result = systemUnderTest.getRelationsAdjacentToElement("bogus")

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsAdjacentToLimitedToClass {
            @Test
            open fun `should return all Relations of the given class that originate from or go to the given Element ID`() {
                val result = systemUnderTest.getRelationsAdjacentToElement(element2.id, SimpleTestRelation::class.java)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation6),
                        mustNotBeContained = setOf(relation2, relation3, relation4, relation5, relation7)
                )
            }

            @Test
            open fun `should return all Relations of the given class and its subclasses that originate from or go to the given Element ID if includeSubClasses is set`() {
                val result = systemUnderTest.getRelationsAdjacentToElement(element1.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation5, relation6),
                        mustNotBeContained = setOf(relation3, relation4, relation7)
                )
            }

            @Test
            open fun `should return an empty result if the ID is not in the Repository`() {
                val result = systemUnderTest.getRelationsAdjacentToElement("bogus", SimpleTestRelation::class.java, true)

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsBetween {
            @Test
            open fun `should return all Relations that are between the given Element IDs`() {
                val result = systemUnderTest.getRelationsBetweenElements(element1.id, element2.id)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation6)
                )
            }

            @Test
            open fun `should not return relations that go in the wrong direction`() {
                val result = systemUnderTest.getRelationsBetweenElements(element1.id, element2.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation5)
                )
            }

            @Test
            open fun `should not return relations that are not adjacent to both elements`() {
                val result = systemUnderTest.getRelationsBetweenElements(element1.id, element2.id)

                assertElementsContained(
                        result,
                        mustNotBeContained = setOf(relation3, relation4, relation7)
                )
            }

            @Test
            open fun `should return an empty result if one of the IDs is not in the Repository`() {
                val result = systemUnderTest.getRelationsBetweenElements("bogus", element1.id)

                assertThat(result).isEmpty()
            }
        }

        @Nested
        open inner class GetRelationsBetweenLimitedToClass {
            @Test
            open fun `should return all Relations of the given class that are between the given Element IDs`() {
                val result = systemUnderTest.getRelationsBetweenElements(element1.id, element2.id, SimpleTestRelation::class.java)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation6),
                        mustNotBeContained = setOf(relation2, relation3, relation4, relation5, relation7)
                )
            }

            @Test
            open fun `should return all Relations of the given class and its subclasses that are between the given Element IDs if includeSubClasses is set`() {
                val result = systemUnderTest.getRelationsBetweenElements(element1.id, element2.id, SimpleTestRelation::class.java, true)

                assertElementsContained(
                        result,
                        mustBeContained = setOf(relation1, relation2, relation6),
                        mustNotBeContained = setOf(relation3, relation4, relation5, relation7)
                )
            }

            @Test
            open fun `should return an empty result if one of the IDs is not in the Repository`() {
                val result = systemUnderTest.getRelationsBetweenElements("bogus", element1.id, SimpleTestRelation::class.java, true)

                assertThat(result).isEmpty()
            }
        }
    }

    protected open fun assertEquivalenceOfInsertedAndStoredElement(insertedElement: Element, storedElement: Element?) {
        assertThat(storedElement).isNotNull.matches {
            it!!.equivalent(insertedElement)
        }
    }

    protected fun assertCorrectnessOfElementQueryResult(input: ElementQueryResult<*>) {
        for ((id, value) in input) {
            assertThat(value.id).isEqualTo(id)
        }
    }

    /**
     * Inserts a [SimpleTestElement] instance to the [systemUnderTest].
     * Then returns the Element instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenASimpleTestElementIsInsertedWith(id: String): SimpleTestElement

    /**
     * Inserts a [SimpleSubclassTestElement] instance to the [systemUnderTest].
     * Then returns the Element instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenASimpleSubclassTestElementIsInsertedWith(id: String): SimpleSubclassTestElement

    /**
     * Inserts a [SimpleSubclassTestElement] instance to the [systemUnderTest].
     * Then returns the Element instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenAnOtherSimpleTestElementIsInsertedWith(id: String): OtherSimpleTestElement

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTest].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenASimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleTestRelation

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTest].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenASimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRefs: Set<ElementReference<*>>,
            bRefs: Set<ElementReference<*>>
    ): SimpleTestRelation

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTest].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenASimpleSubclassTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleSubclassTestRelation

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTest].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    protected abstract fun givenAnOtherSimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): OtherSimpleTestRelation

    protected fun assertElementsContained(
            result: ElementQueryResult<*>,
            mustBeContained: Collection<Element> = setOf(),
            mustNotBeContained: Collection<Element> = setOf()
    ) {
        assertCorrectnessOfElementQueryResult(result)

        for (element in mustBeContained) {
            assertEquivalenceOfInsertedAndStoredElement(element, result[element.id])
        }

        for (element in mustNotBeContained) {
            assertThat(result[element.id]).isNull()
        }
    }

    protected fun givenASimpleElement(id: String): SimpleTestElement {
        return SimpleTestElement(id, "42")
    }

    protected fun givenASimpleSubclassElement(id: String): SimpleSubclassTestElement {
        return SimpleSubclassTestElement(id, "42", "more text")
    }

    protected fun givenAnOtherSimpleElement(id: String): OtherSimpleTestElement {
        return OtherSimpleTestElement(id, "2512")
    }

    protected fun givenASimpleTestRelation(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleTestRelation {
        return SimpleTestRelation(id, "bla", isDirected, setOf(aRef), setOf(bRef))
    }

    protected fun givenASimpleTestRelation(
            id: String,
            isDirected: Boolean,
            aRefs: Set<ElementReference<*>>,
            bRefs: Set<ElementReference<*>>
    ): SimpleTestRelation {
        return SimpleTestRelation(id, "bla", isDirected, aRefs, bRefs)
    }

    protected fun givenASimpleSubclassRelation(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleSubclassTestRelation {
        return SimpleSubclassTestRelation(id, "bla", "blub", isDirected, setOf(aRef), setOf(bRef))
    }

    protected fun givenAnOtherSimpleRelation(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): OtherSimpleTestRelation {
        return OtherSimpleTestRelation(id, "bla", isDirected, setOf(aRef), setOf(bRef))
    }

    protected fun givenATimestamp(): VectorTimestamp {
        return VectorTimestamp(mutableMapOf("x" to 42L))
    }
}
