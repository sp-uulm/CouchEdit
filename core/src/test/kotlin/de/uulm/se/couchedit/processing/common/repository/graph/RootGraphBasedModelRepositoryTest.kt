package de.uulm.se.couchedit.processing.common.repository.graph

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollectionImpl
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryTest
import de.uulm.se.couchedit.processing.common.repository.RelationCache
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.common.services.diff.VersionManager
import de.uulm.se.couchedit.processing.common.testutils.model.OtherSimpleTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleSubclassTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import de.uulm.se.couchedit.util.extensions.ref
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class RootGraphBasedModelRepositoryTest : ModelRepositoryTest() {
    private val diffCollectionFactoryMock: DiffCollectionFactory = mockk()
    private val versionManagerMock: VersionManager = mockk(relaxUnitFun = true)
    private val relationCacheMock: RelationCache = mockk(relaxUnitFun = true)

    override fun getSystemUnderTestInstance(): RootGraphBasedModelRepository {
        return RootGraphBasedModelRepository(diffCollectionFactoryMock, relationCacheMock, versionManagerMock)
    }

    override fun givenTimestampFor(id: String, timestamp: VectorTimestamp) {
        every { versionManagerMock.versionOf(eq(id)) } returns timestamp
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()

        every { diffCollectionFactoryMock.createMutableTimedDiffCollection() } answers { MutableTimedDiffCollectionImpl() }
        every { versionManagerMock.markElementUpdated(any()) } returns VectorTimestamp()
        every { relationCacheMock.onElementInsert(any()) } returns emptySet()
        every { relationCacheMock.insertRelation(any(), any(), any()) } returns true
        every { versionManagerMock.updateVersion(any(), any()) } returns true
    }

    // only test get() for the RootGraphBasedModelRepository as this code is only calling through in the
    // ChildGraphBasedModelRepository, leading to the situation that we would rather test the parent Mock instead of the
    // actual Child implementation

    @Nested
    open inner class GetById {
        @Test
        open fun `should return copy of stored Element after store`() {
            val id = "bla"

            val element = givenASimpleTestElementIsInsertedWith(id)


            val storedValue = systemUnderTest[id]


            assertEquivalenceOfInsertedAndStoredElement(element, storedValue)
        }

        @Test
        open fun `should return null if Element not present`() {
            val id = "bla"
            val otherId = "blub"

            givenASimpleTestElementIsInsertedWith(id)


            val result = systemUnderTest[otherId]


            assertThat(result).isNull()
        }
    }

    @Nested
    open inner class GetByReference {
        @Test
        open fun `should return correct element given an exact reference`() {
            val element = givenASimpleTestElementIsInsertedWith("test")


            val storedValue = systemUnderTest[element.ref()]


            assertEquivalenceOfInsertedAndStoredElement(element, storedValue)
        }

        @Test
        open fun `should return correct element given a reference specifying a superclass`() {
            val id = "subelement"
            val element = givenASimpleSubclassTestElementIsInsertedWith(id)
            val eRef = ElementReference(id, SimpleTestElement::class.java)


            val storedValue = systemUnderTest[eRef]


            assertEquivalenceOfInsertedAndStoredElement(element, storedValue)
        }

        @Test
        open fun `should throw IllegalArgumentException if the type in the Reference is incompatible`() {
            val id = "simpletestelement"

            givenASimpleTestElementIsInsertedWith(id)

            val eRef = ElementReference(id, OtherSimpleTestElement::class.java)


            Assertions.assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest[eRef]
            }
        }

        @Test
        open fun `should throw IllegalArgumentException if the type in the Reference is a subtype`() {
            val id = "subelement"
            givenASimpleTestElementIsInsertedWith(id)

            val eRef = ElementReference(id, SimpleSubclassTestElement::class.java)

            Assertions.assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest[eRef]
            }
        }

        @Test
        open fun `should always return null if null is the parameter`() {
            val id = "subelement"
            givenASimpleTestElementIsInsertedWith(id)


            @Suppress("CAST_NEVER_SUCCEEDS") // needed for type disambiguation
            val result = systemUnderTest[null as? ElementReference<*>]


            assertThat(result).isNull()
        }
    }

    @Nested
    open inner class GetAll {
        @Test
        open fun `should return all Elements that are an instance of the given class`() {
            val elements = listOf(
                    givenASimpleTestElementIsInsertedWith("abc"),
                    givenASimpleTestElementIsInsertedWith("def"),
                    givenASimpleTestElementIsInsertedWith("ghi")
            )


            val result = systemUnderTest.getAll(SimpleTestElement::class.java)


            assertCorrectnessOfElementQueryResult(result)

            for (element in elements) {
                assertEquivalenceOfInsertedAndStoredElement(element, result[element.id])
            }
        }

        @Test
        open fun `should not return Elements that are an instance of a subclass of the given class`() {
            givenASimpleTestElementIsInsertedWith("abc")
            givenASimpleTestElementIsInsertedWith("def")
            givenASimpleTestElementIsInsertedWith("ghi")

            val subclassElementId = "shouldNotBeContained"

            givenASimpleSubclassTestElementIsInsertedWith(subclassElementId)


            val result = systemUnderTest.getAll(SimpleTestElement::class.java)


            assertCorrectnessOfElementQueryResult(result)

            assertThat(result).doesNotContainKey(subclassElementId)
        }

        @Test
        open fun `should not return Elements that are unrelated to the given class`() {
            givenASimpleTestElementIsInsertedWith("abc")
            givenASimpleTestElementIsInsertedWith("def")
            givenASimpleTestElementIsInsertedWith("ghi")

            val otherElementId = "shouldNotBeContained"

            givenAnOtherSimpleTestElementIsInsertedWith(otherElementId)


            val result = systemUnderTest.getAll(SimpleTestElement::class.java)


            assertCorrectnessOfElementQueryResult(result)

            assertThat(result).doesNotContainKey(otherElementId)
        }
    }

    @Nested
    open inner class GetAllIncludingSubTypes {
        @Test
        open fun `should return all Elements that are an instance of the given class or one of its subclasses`() {
            val elements = listOf(
                    givenASimpleTestElementIsInsertedWith("abc"),
                    givenASimpleTestElementIsInsertedWith("def"),
                    givenASimpleSubclassTestElementIsInsertedWith("123"),
                    givenASimpleTestElementIsInsertedWith("ghi"),
                    givenASimpleSubclassTestElementIsInsertedWith("456")
            )


            val result = systemUnderTest.getAllIncludingSubTypes(SimpleTestElement::class.java)


            assertCorrectnessOfElementQueryResult(result)

            for (element in elements) {
                assertEquivalenceOfInsertedAndStoredElement(element, result[element.id])
            }
        }

        @Test
        open fun `should not return Elements that are unrelated to the given class`() {
            givenASimpleTestElementIsInsertedWith("abc")
            givenASimpleTestElementIsInsertedWith("def")
            givenASimpleSubclassTestElementIsInsertedWith("123")
            givenASimpleTestElementIsInsertedWith("ghi")
            givenASimpleSubclassTestElementIsInsertedWith("456")

            val otherElementId = "shouldNotBeContained"

            givenAnOtherSimpleTestElementIsInsertedWith(otherElementId)


            val result = systemUnderTest.getAllIncludingSubTypes(SimpleTestElement::class.java)


            assertCorrectnessOfElementQueryResult(result)

            assertThat(result).doesNotContainKey(otherElementId)
        }
    }

    @Nested
    inner class Dump : ModelRepositoryTest.Dump() {
        @Test
        override fun `should return all Elements and Relations`() {
            every { versionManagerMock.versionOf(any()) } returns VectorTimestamp()

            super.`should return all Elements and Relations`()
        }

        @Test
        fun `should contain correct timestamps`() {
            val id1 = "a"
            val id2 = "b"
            givenASimpleTestElementIsInsertedWith(id1)
            givenASimpleSubclassTestElementIsInsertedWith(id2)

            val timestamp1 = VectorTimestamp(mutableMapOf("Test1" to 11L))
            val timestamp2 = VectorTimestamp(mutableMapOf("Test1" to 99L, "Test2" to 32168L))
            every { versionManagerMock.versionOf(eq(id1)) } returns timestamp1
            every { versionManagerMock.versionOf(eq(id2)) } returns timestamp2


            val result = systemUnderTestWritable.dump()


            assertThat(result.getVersionForElement(id1)).isEqualTo(timestamp1)
            assertThat(result.getVersionForElement(id2)).isEqualTo(timestamp2)
        }
    }

    @Nested
    inner class Store : ModelRepositoryTest.Store() {
        @Test
        fun `should register local event and use the return value of VersionManager_markElementUpdated if no other timestamp given`() {
            val id = "test"

            val input = givenASimpleElement(id)

            val timestamp = givenATimestamp()

            every { versionManagerMock.markElementUpdated(any()) } returns timestamp


            val result = systemUnderTestWritable.store(input)


            verify { versionManagerMock.registerLocalEvent() }

            assertThat(result.getVersionForElement(id)).isEqualTo(timestamp)
        }

        @Test
        fun `should update the timestamp in the VersionManager if a timestamp is provided to store`() {
            val id = "test"
            val input = givenASimpleElement(id)
            val timestamp = givenATimestamp()


            val result = systemUnderTestWritable.store(input, timestamp)


            assertThat(result.getVersionForElement(id)).isEqualTo(timestamp)
            verify(exactly = 1) { versionManagerMock.updateVersion(eq(id), eq(timestamp)) }
            verify(exactly = 0) {
                versionManagerMock.registerLocalEvent()
                versionManagerMock.markElementUpdated(any())
            }

            confirmVerified(versionManagerMock)
        }

        @Nested
        inner class StoreRelations : ModelRepositoryTest.Store.StoreRelations() {
            @Test
            fun `should insert a relation into the RelationCache if its dependencies are not yet present`() {
                val aRef = ElementReference("foo", Element::class.java)
                val bRef = ElementReference("bar", Element::class.java)

                val input = givenASimpleTestRelation("id", true, aRef, bRef)

                val timestamp = VectorTimestamp(mutableMapOf("abc" to 11L))


                systemUnderTestWritable.store(input, timestamp)


                verify {
                    relationCacheMock.insertRelation(
                            match { it.equivalent(input) },
                            eq(timestamp),
                            eq(setOf(aRef, bRef))
                    )
                }
            }

            @Test
            fun `should also insert the dependent relations with their original timestamp when all dependencies are satisfied`() {
                val aElement = givenASimpleElement("foo")
                val bElement = givenASimpleElement("bar")

                val input = givenASimpleTestRelation("xyz", true, aElement.ref(), bElement.ref())
                val inputTimestamp = givenATimestamp()

                val missingReferences = mutableSetOf<ElementReference<*>>()

                // mock RelationCache behavior: "Releases" relation after all dependencies have been onElementInsert()ed
                every { relationCacheMock.insertRelation(any(), any(), any()) } answers {
                    missingReferences.addAll(arg(2))
                }

                every { relationCacheMock.onElementInsert(any()) } answers {
                    missingReferences.remove(arg(0))

                    if (missingReferences.isEmpty()) {
                        setOf(Pair(input.copy(), inputTimestamp))
                    } else setOf()
                }


                val result1 = systemUnderTestWritable.store(input, inputTimestamp)
                val result2 = systemUnderTestWritable.store(aElement)
                val result3 = systemUnderTestWritable.store(bElement)


                assertThat(result1.size).isEqualTo(0)
                assertThat(result2.size).isEqualTo(1)
                assertThat(result3.size)
                        .describedAs("// When the last dependency is inserted, the relation should also be inserted")
                        .isEqualTo(2)

                val diff = result3.getDiffForElement(input.ref())

                assertThat(diff).isInstanceOf(ElementAddDiff::class.java)
                assertThat(diff!!.affected).isNotSameAs(input).matches {
                    it.equivalent(input)
                }
            }
        }
    }

    @Nested
    inner class Remove : ModelRepositoryTest.Remove() {
        @Test
        fun `should notify RelationCache of removal`() {
            val id = "test"

            val element = givenASimpleTestElementIsInsertedWith(id)

            systemUnderTestWritable.remove(id)

            verify {
                relationCacheMock.onElementRemove(eq(element.ref()))
            }
        }

        @Test
        fun `should notify VersionManager of removal`() {
            val id = "test"

            givenASimpleTestElementIsInsertedWith(id)

            systemUnderTestWritable.remove(id)

            verify {
                versionManagerMock.onRemove(eq(id))
            }
        }
    }
}
