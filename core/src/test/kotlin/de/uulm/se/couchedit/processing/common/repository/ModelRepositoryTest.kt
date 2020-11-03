package de.uulm.se.couchedit.processing.common.repository

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.testutils.model.*
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Base class to test implementations of the R/W [ModelRepository] interface
 */
@Suppress("unused")
abstract class ModelRepositoryTest : ModelRepositoryReadTest() {
    val systemUnderTestWritable: ModelRepository
        get() = this.systemUnderTest as ModelRepository

    abstract override fun getSystemUnderTestInstance(): ModelRepository

    abstract fun givenTimestampFor(id: String, timestamp: VectorTimestamp)

    @Nested
    open inner class Dump {
        @Test
        open fun `should return all Elements and Relations`() {
            val id1 = "a"
            val id2 = "b"
            val id3 = "c"

            val element1 = givenASimpleSubclassTestElementIsInsertedWith(id1)
            val element2 = givenAnOtherSimpleTestElementIsInsertedWith(id2)
            val element3 = givenASimpleTestElementIsInsertedWith(id3)

            val nonRelatedElement = givenASimpleTestElementIsInsertedWith("xyz")

            val relationId1 = "relation1"
            val relationId2 = "relation2"
            val relation1 = givenASimpleTestRelationIsInsertedWith(relationId1, true, element1.ref(), element2.ref())
            val relation2 = givenASimpleTestRelationIsInsertedWith(relationId2, true, element3.ref(), relation1.ref())

            val elements = listOf(element1, element2, element3, nonRelatedElement, relation1, relation2)


            val result = systemUnderTestWritable.dump()


            for (element in elements) {
                val diff = result.getDiffForElement(element.ref())

                assertThat(diff).isInstanceOf(ElementAddDiff::class.java)

                assertEquivalenceOfInsertedAndStoredElement(element, diff!!.affected)
            }
        }
    }

    @Nested
    open inner class Store {
        @Test
        open fun `should return ElementAddDiff after first store`() {
            val input = givenASimpleElement("test")


            val result = systemUnderTestWritable.store(input)


            assertThat(result.size).isEqualTo(1)
            val diff = result.first()

            assertThat(diff).isInstanceOf(ElementAddDiff::class.java)

            assertDiffElementPropertiesEqual(input, diff)
        }

        @Test
        open fun `should return ElementModifyDiff after subsequent stores with same ID and type`() {
            val input = givenASimpleElement("test")

            // store the first time
            systemUnderTestWritable.store(input)

            input.x = "1337"


            val result = systemUnderTestWritable.store(input)


            assertThat(result.size).isEqualTo(1)
            val diff = result.first()

            assertThat(diff).isInstanceOf(ElementModifyDiff::class.java)

            assertDiffElementPropertiesEqual(input, diff)
        }

        @Test
        open fun `should reject store with same ID, but different type`() {
            val id = "test"

            val input = givenASimpleElement(id)
            val input2 = givenAnOtherSimpleElement(id)

            // store the first time
            systemUnderTestWritable.store(input)


            assertThatIllegalArgumentException().isThrownBy { systemUnderTestWritable.store(input2) }
        }

        @Test
        open fun `should return empty DiffCollection if an equivalent element is already stored`() {
            val input = givenASimpleElement("test")

            // store the first time
            systemUnderTestWritable.store(input)

            val input2 = input.copy()


            val result = systemUnderTestWritable.store(input2)


            assertThat(result.size).isEqualTo(0)
        }

        @Nested
        open inner class StoreRelations {
            @Test
            open fun `should correctly insert relation if dependencies are in the repository`() {
                val aElement = givenASimpleElement("foo")
                val bElement = givenASimpleElement("bar")

                systemUnderTestWritable.store(aElement)
                systemUnderTestWritable.store(bElement)

                val input = givenASimpleTestRelation(
                        "test",
                        true,
                        aElement.ref(),
                        bElement.ref()
                )


                val result = systemUnderTestWritable.store(input)

                assertThat(result.size).isEqualTo(1)

                val diff = result.first()

                assertDiffElementPropertiesEqual(input, diff)
            }

            @Test
            open fun `should not insert a relation of which the dependencies are not yet inserted`() {
                val id = "bla"

                val input = givenASimpleTestRelation(
                        id,
                        true,
                        ElementReference("foo", Element::class.java),
                        ElementReference("bar", Element::class.java)
                )


                val result = systemUnderTestWritable.store(input)


                assertThat(result.size).isEqualTo(0)

                assertThat(systemUnderTestWritable[id]).isNull()
            }

            @Test
            open fun `should reject relation update if relation ends would be changed`() {
                val aElement = givenASimpleElement("foo")
                val bElement = givenASimpleElement("bar")
                val cElement = givenASimpleElement("baz")

                systemUnderTestWritable.store(aElement)
                systemUnderTestWritable.store(bElement)
                systemUnderTestWritable.store(cElement)

                val id = "test"

                val rel1 = givenASimpleTestRelation(
                        id,
                        true,
                        aElement.ref(),
                        bElement.ref()
                )

                val rel2 = givenASimpleTestRelation(
                        id,
                        true,
                        aElement.ref(),
                        cElement.ref()
                )

                systemUnderTestWritable.store(rel1)


                assertThatIllegalArgumentException().isThrownBy {
                    systemUnderTestWritable.store(rel2)
                }
            }

            @Test
            open fun `should reject relation update if relation ends would be swapped`() {
                val aElement = givenASimpleElement("foo")
                val bElement = givenASimpleElement("bar")

                systemUnderTestWritable.store(aElement)
                systemUnderTestWritable.store(bElement)

                val id = "test"

                val rel1 = givenASimpleTestRelation(
                        id,
                        true,
                        aElement.ref(),
                        bElement.ref()
                )

                val rel2 = givenASimpleTestRelation(
                        id,
                        true,
                        bElement.ref(),
                        aElement.ref()
                )

                systemUnderTestWritable.store(rel1)


                assertThatIllegalArgumentException().isThrownBy {
                    systemUnderTestWritable.store(rel2)
                }
            }

            @Test
            open fun `should reject relation update if relation directedness would be changed`() {
                val aElement = givenASimpleElement("foo")
                val bElement = givenASimpleElement("bar")

                systemUnderTestWritable.store(aElement)
                systemUnderTestWritable.store(bElement)

                val id = "test"

                val rel1 = givenASimpleTestRelation(
                        id,
                        true,
                        aElement.ref(),
                        bElement.ref()
                )

                val rel2 = givenASimpleTestRelation(
                        id,
                        false,
                        aElement.ref(),
                        bElement.ref()
                )

                systemUnderTestWritable.store(rel1)


                assertThatIllegalArgumentException().isThrownBy {
                    systemUnderTestWritable.store(rel2)
                }
            }
        }

        private fun assertDiffElementPropertiesEqual(input: Element, diff: ModelDiff) {
            assertThat(diff.affected).describedAs(
                    "Input element must be a different instance than that returned in the ModelDiff"
            ).isNotSameAs(input.id)
            assertThat(diff.affected.id).isEqualTo(input.id)
            assert(diff.affected.equivalent(input))
        }
    }

    @Nested
    open inner class Remove {
        @Test
        open fun `should return ElementRemoveDiff after first remove`() {
            val id = "test"

            givenASimpleTestElementIsInsertedWith(id)


            val result = systemUnderTestWritable.remove(id)


            assertThat(result.size).isEqualTo(1)

            val diff = result.first()

            assertThat(diff.affected.id).isEqualTo(id)
        }

        @Test
        open fun `should return empty DiffCollection if element not present`() {
            val id = "test"


            val result = systemUnderTestWritable.remove(id)


            assertThat(result.size).isEqualTo(0)
        }

        @Test
        open fun `should remove all dependent relations along with the element`() {
            val aId = "foo"
            val bId = "bar"

            val aElement = givenASimpleElement(aId)
            val bElement = givenASimpleElement(bId)

            systemUnderTestWritable.store(aElement)
            systemUnderTestWritable.store(bElement)

            val idIncoming = "test1"
            val idOutgoing = "test2"
            val idBidirectional = "test3"

            val tsIncoming = VectorTimestamp(mutableMapOf("1" to 1L, "2" to 2L, "3" to 3L))
            val tsOutgoing = VectorTimestamp(mutableMapOf("4" to 4L, "5" to 5L, "6" to 6L))
            val tsBidirectional = VectorTimestamp(mutableMapOf("7" to 7L, "8" to 8L, "9" to 9L))

            givenTimestampFor(idIncoming, tsIncoming)
            givenTimestampFor(idOutgoing, tsOutgoing)
            givenTimestampFor(idBidirectional, tsBidirectional)

            val incomingRelation = givenASimpleTestRelation(
                    idIncoming,
                    true,
                    bElement.ref(),
                    aElement.ref()
            )

            val outgoingRelation = givenASimpleTestRelation(
                    idOutgoing,
                    true,
                    aElement.ref(),
                    bElement.ref()
            )

            val bidirectionalRelation = givenASimpleTestRelation(
                    idBidirectional,
                    false,
                    aElement.ref(),
                    bElement.ref()
            )

            systemUnderTestWritable.store(incomingRelation)
            systemUnderTestWritable.store(outgoingRelation)
            systemUnderTestWritable.store(bidirectionalRelation)


            val result = systemUnderTestWritable.remove(aId)


            assertThat(result.size).isEqualTo(4)

            assertThat(result.getDiffForElement(incomingRelation.ref())).isInstanceOf(ElementRemoveDiff::class.java)
            assertThat(result.getDiffForElement(outgoingRelation.ref())).isInstanceOf(ElementRemoveDiff::class.java)
            assertThat(result.getDiffForElement(bidirectionalRelation.ref())).isInstanceOf(ElementRemoveDiff::class.java)

            assertThat(systemUnderTestWritable[idIncoming]).isNull()
            assertThat(systemUnderTestWritable[idOutgoing]).isNull()
            assertThat(systemUnderTestWritable[idBidirectional]).isNull()
        }
    }

    @Nested
    inner class Clear {
        @Test
        fun `should remove all Elements from the ModelRepository`() {
            val aId = "foo"
            val bId = "bar"
            val cId = "baz"
            val relId = "rel1"

            val relTs = VectorTimestamp(mutableMapOf("1" to 1L, "2" to 2L, "3" to 3L))

            givenTimestampFor(relId, relTs)

            val aElement = givenASimpleElement(aId)
            val bElement = givenASimpleElement(bId)
            val cElement = givenASimpleElement(cId)

            systemUnderTestWritable.store(aElement)
            systemUnderTestWritable.store(bElement)
            systemUnderTestWritable.store(cElement)

            val relation = givenASimpleTestRelation(
                    relId,
                    true,
                    bElement.ref(),
                    aElement.ref()
            )

            systemUnderTestWritable.store(relation)


            val result = systemUnderTestWritable.clear()


            assertThat(systemUnderTestWritable.getAllIncludingSubTypes(Element::class.java)).isEmpty()

            assertThat(result.getDiffForElement(aElement.ref())).isInstanceOf(ElementRemoveDiff::class.java)
            assertThat(result.getDiffForElement(bElement.ref())).isInstanceOf(ElementRemoveDiff::class.java)
            assertThat(result.getDiffForElement(cElement.ref())).isInstanceOf(ElementRemoveDiff::class.java)
            assertThat(result.getDiffForElement(relation.ref())).isInstanceOf(ElementRemoveDiff::class.java)
        }
    }

    @Nested
    inner class ChangeListener {
        @Test
        fun `should get notified on element insert after addOnChangeListener`() {
            var recordedModelDiff: ModelDiff? = null

            val changeListener = { x: ModelDiff ->
                recordedModelDiff = x
            }

            systemUnderTestWritable.addOnChangeListener("blub", changeListener)

            val element = givenASimpleElement("test")


            systemUnderTestWritable.store(element)


            assertThat(recordedModelDiff).isInstanceOf(ElementAddDiff::class.java)
            assertThat(recordedModelDiff!!.affected).isNotSameAs(element).matches {
                it.equivalent(element)
            }
        }

        @Test
        fun `should get notified on element remove after addOnChangeListener`() {
            var recordedModelDiff: ModelDiff? = null

            val changeListener = { x: ModelDiff ->
                recordedModelDiff = x
            }

            val id = "test"
            val element = givenASimpleTestElementIsInsertedWith(id)

            systemUnderTestWritable.addOnChangeListener("blub", changeListener)


            systemUnderTestWritable.remove(id)


            assertThat(recordedModelDiff).isInstanceOf(ElementRemoveDiff::class.java)
            assertThat(recordedModelDiff!!.affected).isNotSameAs(element).matches {
                it.equivalent(element)
            }
        }

        @Test
        fun `should get notified on element modify after addOnChangeListener`() {
            var recordedModelDiff: ModelDiff? = null

            val changeListener = { x: ModelDiff ->
                recordedModelDiff = x
            }

            val id = "test"
            val element = givenASimpleTestElementIsInsertedWith(id)

            systemUnderTestWritable.addOnChangeListener("blub", changeListener)

            element.x = "1337"


            systemUnderTestWritable.store(element)


            assertThat(recordedModelDiff).isInstanceOf(ElementModifyDiff::class.java)
            assertThat(recordedModelDiff!!.affected).isNotSameAs(element).matches {
                it.equivalent(element)
            }
        }

        @Test
        fun `should no longer get notified after removeOnChangeListener`() {
            var recordedModelDiff: ModelDiff? = null

            val changeListener = { x: ModelDiff ->
                recordedModelDiff = x
            }

            val changeListenerId = "blub"

            systemUnderTestWritable.addOnChangeListener(changeListenerId, changeListener)

            val element = givenASimpleElement("test")

            systemUnderTestWritable.store(element)

            // check that the change listener worked
            assertThat(recordedModelDiff).isInstanceOf(ElementAddDiff::class.java)
            assertThat(recordedModelDiff!!.affected).isNotSameAs(element).matches {
                it.equivalent(element)
            }

            // now for the actual test:


            recordedModelDiff = null

            val element2 = givenAnOtherSimpleElement("test2")

            systemUnderTestWritable.removeOnChangeListener(changeListenerId)


            systemUnderTestWritable.store(element2)


            @Suppress("USELESS_CAST") // cast needed for interop ambiguity
            assertThat(recordedModelDiff as ModelDiff?).isNull()
        }
    }

    /**
     * Inserts a [SimpleTestElement] instance to the [systemUnderTestWritable].
     * Then returns the Element instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    override fun givenASimpleTestElementIsInsertedWith(id: String): SimpleTestElement {
        val element = givenASimpleElement(id)

        systemUnderTestWritable.store(element)

        return element
    }

    /**
     * Inserts a [SimpleSubclassTestElement] instance to the [systemUnderTestWritable].
     * Then returns the Element instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    override fun givenASimpleSubclassTestElementIsInsertedWith(id: String): SimpleSubclassTestElement {
        val element = givenASimpleSubclassElement(id)

        systemUnderTestWritable.store(element)

        return element
    }

    /**
     * Inserts a [SimpleSubclassTestElement] instance to the [systemUnderTestWritable].
     * Then returns the Element instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    override fun givenAnOtherSimpleTestElementIsInsertedWith(id: String): OtherSimpleTestElement {
        val element = givenAnOtherSimpleElement(id)

        systemUnderTestWritable.store(element)

        return element
    }

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTestWritable].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    override fun givenASimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleTestRelation {
        val relation = givenASimpleTestRelation(id, isDirected, aRef, bRef)

        systemUnderTestWritable.store(relation)

        return relation
    }

    override fun givenASimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRefs: Set<ElementReference<*>>,
            bRefs: Set<ElementReference<*>>
    ): SimpleTestRelation {
        val relation = givenASimpleTestRelation(id, isDirected, aRefs, bRefs)

        systemUnderTestWritable.store(relation)

        return relation
    }

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTestWritable].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    override fun givenASimpleSubclassTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): SimpleSubclassTestRelation {
        val relation = givenASimpleSubclassRelation(id, isDirected, aRef, bRef)

        systemUnderTestWritable.store(relation)

        return relation
    }

    /**
     * Inserts a [SimpleTestRelation] instance to the [systemUnderTestWritable].
     * Then returns the Relation instance as **inserted**, i.e. the original instance passed to [ModelRepository.store]
     */
    override fun givenAnOtherSimpleTestRelationIsInsertedWith(
            id: String,
            isDirected: Boolean,
            aRef: ElementReference<*>,
            bRef: ElementReference<*>
    ): OtherSimpleTestRelation {
        val relation = givenAnOtherSimpleRelation(id, isDirected, aRef, bRef)

        systemUnderTestWritable.store(relation)

        return relation
    }

    override fun assertEquivalenceOfInsertedAndStoredElement(insertedElement: Element, storedElement: Element?) {
        super.assertEquivalenceOfInsertedAndStoredElement(insertedElement, storedElement)

        // As we insert the Elements via the normal store() function, also assert that the instances are not the same
        assertThat(storedElement).isNotSameAs(insertedElement)
    }
}
