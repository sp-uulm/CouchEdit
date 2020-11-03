package de.uulm.se.couchedit.processing.common.repository.queries

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestOneToOneRelation
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestRelation
import de.uulm.se.couchedit.util.extensions.ref
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RelationGraphQueriesTest {
    private val modelRepositoryMock: ModelRepository = mockk()

    private val systemUnderTest: RelationGraphQueries = RelationGraphQueries(modelRepositoryMock)

    @Nested
    inner class GetElementRelatedFrom {
        @Test
        fun `should return value of b of single relation as given by the ModelRepository`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test2")

            val abRelation = SimpleTestOneToOneRelation(aRef, b.ref(), true)

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(abRelation.id to abRelation))

            givenModelRepositoryGetReturnsCorrectElement(b)


            val result = systemUnderTest.getElementRelatedFrom(aRef, SimpleTestOneToOneRelation::class.java, true)


            assertThat(result).isNotNull.matches { it!!.equivalent(b) }
        }

        @Test
        fun `should return null if no relation is given by the ModelRepository`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(emptyMap())


            val result = systemUnderTest.getElementRelatedFrom(aRef, SimpleTestOneToOneRelation::class.java, true)


            assertThat(result).isNull()
        }

        @Test
        fun `should throw an IllegalStateException if more than one relation is found`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test")
            val c = SimpleTestElement("c", "test2")

            val abRelation = SimpleTestOneToOneRelation(aRef, b.ref(), true)
            val acRelation = SimpleTestOneToOneRelation(aRef, c.ref(), true)

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    abRelation.id to abRelation,
                    acRelation.id to acRelation
            ))


            assertThatIllegalStateException().isThrownBy {
                systemUnderTest.getElementRelatedFrom(aRef, SimpleTestOneToOneRelation::class.java, true)
            }
        }

        @Test
        fun `should throw an IllegalArgumentException if the found relation is undirected`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test")

            val abRelation = SimpleTestOneToOneRelation(aRef, b.ref(), false)

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(abRelation.id to abRelation))


            assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest.getElementRelatedFrom(aRef, SimpleTestOneToOneRelation::class.java, true)
            }
        }
    }

    @Nested
    inner class GetElementRelatedTo {
        @Test
        fun `should return value of a of single relation as given by the ModelRepository`() {
            val a = SimpleTestElement("a", "test")
            val bRef = ElementReference("b", SimpleTestElement::class.java)

            val abRelation = SimpleTestOneToOneRelation(a.ref(), bRef, true)

            every {
                modelRepositoryMock.getRelationsToElement(
                        bRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(abRelation.id to abRelation))

            givenModelRepositoryGetReturnsCorrectElement(a)


            val result = systemUnderTest.getElementRelatedTo(bRef, SimpleTestOneToOneRelation::class.java, true)


            assertThat(result).isNotNull.matches { it!!.equivalent(a) }
        }

        @Test
        fun `should return null if no relation is given by the ModelRepository`() {
            val bRef = ElementReference("b", SimpleTestElement::class.java)

            every {
                modelRepositoryMock.getRelationsToElement(
                        bRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(emptyMap())


            val result = systemUnderTest.getElementRelatedTo(bRef, SimpleTestOneToOneRelation::class.java, true)


            assertThat(result).isNull()
        }

        @Test
        fun `should throw an IllegalStateException if more than one relation is found`() {
            val a = SimpleTestElement("b", "test")
            val bRef = ElementReference("b", SimpleTestElement::class.java)
            val c = SimpleTestElement("c", "test2")

            val abRelation = SimpleTestOneToOneRelation(a.ref(), bRef, true)
            val cbRelation = SimpleTestOneToOneRelation(c.ref(), bRef, true)

            every {
                modelRepositoryMock.getRelationsToElement(
                        bRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    abRelation.id to abRelation,
                    cbRelation.id to cbRelation
            ))


            assertThatIllegalStateException().isThrownBy {
                systemUnderTest.getElementRelatedTo(bRef, SimpleTestOneToOneRelation::class.java, true)
            }
        }

        @Test
        fun `should throw an IllegalArgumentException if the found relation is undirected`() {
            val a = SimpleTestElement("a", "test")
            val bRef = ElementReference("b", SimpleTestElement::class.java)

            val abRelation = SimpleTestOneToOneRelation(a.ref(), bRef, false)

            every {
                modelRepositoryMock.getRelationsToElement(
                        bRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(abRelation.id to abRelation))


            assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest.getElementRelatedTo(bRef, SimpleTestOneToOneRelation::class.java, true)
            }
        }
    }

    @Nested
    inner class GetElementsRelatedFrom {
        @Test
        fun `should return b values of multiple OneToOneRelations`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test")
            val c = SimpleTestElement("c", "test2")

            val abRelation = SimpleTestOneToOneRelation(aRef, b.ref(), true)
            val acRelation = SimpleTestOneToOneRelation(aRef, c.ref(), true)

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    abRelation.id to abRelation,
                    acRelation.id to acRelation
            ))

            givenModelRepositoryGetReturnsCorrectElement(b, c)


            val result = systemUnderTest.getElementsRelatedFrom(aRef, SimpleTestOneToOneRelation::class.java, true)


            assertThat(result).contains(b, c)
        }

        @Test
        fun `should return b values of One-To-Many Relations`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test1")
            val c = SimpleTestElement("c", "test2")
            val d = SimpleTestElement("d", "test3")
            val e = SimpleTestElement("e", "test4")

            val abcRelation = SimpleTestRelation("rTest", "x", true, setOf(aRef), setOf(b.ref(), c.ref()))
            val adeRelation = SimpleTestRelation("rTest2", "x", true, setOf(aRef), setOf(d.ref(), e.ref()))

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    abcRelation.id to abcRelation,
                    adeRelation.id to adeRelation
            ))

            givenModelRepositoryGetReturnsCorrectElement(b, c, d, e)


            val result = systemUnderTest.getElementsRelatedFrom(aRef, SimpleTestRelation::class.java, true)


            assertThat(result).contains(b, c, d, e)
        }

        @Test
        fun `should throw an IllegalArgumentException if one of the found relations is undirected`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test")
            val c = SimpleTestElement("c", "test2")
            // this reference should not be contained in the output because it is an "a" reference.
            // If the systemUnderTest would try to access it, it would give a mockk error.
            val xRef = ElementReference("notContained", SimpleTestElement::class.java)


            val abRelation = SimpleTestRelation("test1", "x", true, setOf(aRef, xRef), setOf(b.ref()))
            val abcRelation = SimpleTestRelation("test2", "x", false, setOf(aRef), setOf(b.ref(), c.ref()))

            every {
                modelRepositoryMock.getRelationsFromElement(
                        aRef.id,
                        eq(SimpleTestRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    abRelation.id to abRelation,
                    abcRelation.id to abcRelation
            ))

            givenModelRepositoryGetReturnsCorrectElement(b, c)


            assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest.getElementsRelatedFrom(aRef, SimpleTestRelation::class.java, true)
            }
        }
    }

    @Nested
    inner class GetElementsRelatedTo {
        @Test
        fun `should return a values of multiple OneToOneRelations`() {
            val a = SimpleTestElement("a", "test")
            val bRef = ElementReference("b", SimpleTestElement::class.java)
            val c = SimpleTestElement("c", "test2")

            val abRelation = SimpleTestOneToOneRelation(a.ref(), bRef, true)
            val cbRelation = SimpleTestOneToOneRelation(c.ref(), bRef, true)

            every {
                modelRepositoryMock.getRelationsToElement(
                        bRef.id,
                        eq(SimpleTestOneToOneRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    abRelation.id to abRelation,
                    cbRelation.id to cbRelation
            ))

            givenModelRepositoryGetReturnsCorrectElement(a, c)


            val result = systemUnderTest.getElementsRelatedTo(bRef, SimpleTestOneToOneRelation::class.java, true)


            assertThat(result).contains(a, c)
        }

        @Test
        fun `should return a values of One-To-Many Relations`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test1")
            val c = SimpleTestElement("c", "test2")
            val d = SimpleTestElement("d", "test3")
            val e = SimpleTestElement("e", "test4")

            // this reference should not be contained in the output because it is an "a" reference.
            // If the systemUnderTest would try to access it, it would give a mockk error.
            val xRef = ElementReference("notContained", SimpleTestElement::class.java)

            val bcaRelation = SimpleTestRelation("rTest", "x", true, setOf(b.ref(), c.ref()), setOf(aRef, xRef))
            val deaRelation = SimpleTestRelation("rTest2", "x", true, setOf(d.ref(), e.ref()), setOf(aRef))

            every {
                modelRepositoryMock.getRelationsToElement(
                        aRef.id,
                        eq(SimpleTestRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    bcaRelation.id to bcaRelation,
                    deaRelation.id to deaRelation
            ))

            givenModelRepositoryGetReturnsCorrectElement(b, c, d, e)


            val result = systemUnderTest.getElementsRelatedTo(aRef, SimpleTestRelation::class.java, true)


            assertThat(result).contains(b, c, d, e)
        }

        @Test
        fun `should throw an IllegalArgumentException if one of the found relations is undirected`() {
            val aRef = ElementReference("a", SimpleTestElement::class.java)
            val b = SimpleTestElement("b", "test")
            val c = SimpleTestElement("c", "test2")


            val baRelation = SimpleTestRelation("test1", "x", true, setOf(b.ref()), setOf(aRef))
            val bcaRelation = SimpleTestRelation("test2", "x", false, setOf(b.ref(), c.ref()), setOf(aRef))

            every {
                modelRepositoryMock.getRelationsToElement(
                        aRef.id,
                        eq(SimpleTestRelation::class.java),
                        eq(true)
                )
            } returns ElementQueryResult(mapOf(
                    baRelation.id to baRelation,
                    bcaRelation.id to bcaRelation
            ))

            givenModelRepositoryGetReturnsCorrectElement(b, c)


            assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest.getElementsRelatedTo(aRef, SimpleTestRelation::class.java, true)
            }
        }
    }

    /**
     * Configures the [modelRepositoryMock] so that [element] can be retrieved via the [ModelRepository.get] calls
     */
    private fun givenModelRepositoryGetReturnsCorrectElement(vararg elements: Element) {
        for (element in elements) {
            every {
                modelRepositoryMock[eq(element.id)]
                modelRepositoryMock[eq(element.ref())]
            } returns element
        }

    }
}
