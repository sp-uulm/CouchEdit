package de.uulm.se.couchedit.processing.common.repository

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestRelation
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RelationCacheTest {
    private val systemUnderTest = RelationCache()

    @Nested
    inner class InsertRelation {
        @Test
        fun `should return true if a new Relation is inserted`() {
            val ref1 = givenAnElementReference1()
            val ref2 = givenAnElementReference2()

            val missingElements = setOf(ref1, ref2)

            val relation = givenARelation("test", setOf(ref1), setOf(ref2))

            val timestamp = givenANewerTimestamp()


            val result = systemUnderTest.insertRelation(relation, timestamp, missingElements)


            assertThat(result).isTrue()
        }

        @Test
        fun `should return false if a relation with an older timestamp is inserted`() {
            val ref1 = givenAnElementReference1()
            val ref2 = givenAnElementReference2()

            val missingElements = setOf(ref1, ref2)

            val relation = givenARelation("test", setOf(ref1), setOf(ref2))

            val firstTimestamp = givenANewerTimestamp()
            val secondTimestamp = givenAnOlderTimestamp()


            val result1 = systemUnderTest.insertRelation(relation, firstTimestamp, missingElements)
            val result2 = systemUnderTest.insertRelation(relation, secondTimestamp, missingElements)


            assertThat(result1).isTrue()
            assertThat(result2).isFalse()
        }

        /**
         * Users of [RelationCache] are supposed to call [RelationCache.onElementRemove] on every Element removal,
         * which should then remove all dependent Relations from the Cache.
         * Thus it is an illegal operation to try and insert an already contained Relation again with a different
         * (bigger) set of Relations.
         */
        @Test
        fun `should reject new missingElements items on subsequent calls`() {
            val ref1 = givenAnElementReference1()
            val ref2 = givenAnElementReference2()

            val originalMissingElements = setOf(ref1)

            val relation = givenARelation("test", setOf(ref1), setOf(ref2))

            val timestamp = givenANewerTimestamp()

            systemUnderTest.insertRelation(relation, timestamp, originalMissingElements)

            val newMissingElements = setOf(ref1, ref2)

            assertThatIllegalArgumentException().isThrownBy {
                systemUnderTest.insertRelation(relation, timestamp, newMissingElements)
            }
        }
    }

    @Nested
    inner class OnElementInsert : RelationInitializingTestCase() {
        @Test
        fun `should return an empty set as long as the missing dependencies have not been fully inserted`() {
            val result = systemUnderTest.onElementInsert(ref1)


            assertThat(result).isEmpty()
        }

        @Test
        fun `should return the waiting relations if all missing dependencies have been inserted`() {
            systemUnderTest.onElementInsert(ref1)


            val result = systemUnderTest.onElementInsert(ref2)


            assertThat(result).hasSize(2)

            assertThat(result).contains(
                    Pair(relation1, timestamp1),
                    Pair(relation2, timestamp2)
            )
        }
    }

    @Nested
    inner class OnElementRemove : RelationInitializingTestCase() {
        @Test
        fun `should remove all Relations waiting for that Element if given a regular Element`() {
            systemUnderTest.onElementRemove(ref1)

            systemUnderTest.onElementInsert(ref1)
            val result = systemUnderTest.onElementInsert(ref2)


            assertThat(result).isEmpty()
        }

        @Test
        fun `should remove that relation if given a Relation Element`() {
            systemUnderTest.onElementRemove(relation1.ref())


            systemUnderTest.onElementInsert(ref1)
            val result = systemUnderTest.onElementInsert(ref2)


            assertThat(result).hasSize(1)

            assertThat(result).contains(Pair(relation2, timestamp2))
        }
    }

    abstract inner class RelationInitializingTestCase {
        lateinit var ref1: ElementReference<*>
        lateinit var ref2: ElementReference<*>

        lateinit var missingElements: Set<ElementReference<*>>
        lateinit var timestamp1: VectorTimestamp
        lateinit var timestamp2: VectorTimestamp
        lateinit var relation1: Relation<*, *>
        lateinit var relation2: Relation<*, *>

        @BeforeEach
        fun setUp() {
            ref1 = givenAnElementReference1()
            ref2 = givenAnElementReference2()

            missingElements = setOf(ref1, ref2)

            timestamp1 = givenANewerTimestamp()
            timestamp2 = givenAnOlderTimestamp()

            relation1 = givenARelation("test", setOf(ref1), setOf(ref2))
            relation2 = givenARelation("test2", setOf(ref1), setOf(ref2))

            systemUnderTest.insertRelation(relation1, timestamp1, missingElements)
            systemUnderTest.insertRelation(relation2, timestamp2, missingElements)
        }
    }

    @Nested
    inner class VersionOf {
        @Test
        fun `should return timestamp of last insertRelation if that returned true`() {
            val ref1 = givenAnElementReference1()
            val ref2 = givenAnElementReference2()

            val missingElements = setOf(ref1, ref2)

            val relation = givenARelation("test", setOf(ref1), setOf(ref2))

            val timestamp = givenANewerTimestamp()

            systemUnderTest.insertRelation(relation, timestamp, missingElements)


            val resultById = systemUnderTest.versionOf(relation.id)
            val resultByRef = systemUnderTest.versionOf(relation.ref())


            assertThat(resultById).isEqualTo(resultByRef)
            assertThat(resultByRef).isEqualTo(timestamp)
        }

        @Test
        fun `should still return previous insertRelation timestamp that returned true`() {
            val ref1 = givenAnElementReference1()
            val ref2 = givenAnElementReference2()

            val missingElements = setOf(ref1, ref2)

            val relation = givenARelation("test", setOf(ref1), setOf(ref2))

            val timestamp = givenANewerTimestamp()
            val timestamp2 = givenAnOlderTimestamp()

            systemUnderTest.insertRelation(relation, timestamp, missingElements)
            val result2 = systemUnderTest.insertRelation(relation, timestamp2, missingElements)


            val resultById = systemUnderTest.versionOf(relation.id)
            val resultByRef = systemUnderTest.versionOf(relation.ref())


            assertThat(result2).isFalse()
            assertThat(resultById).isEqualTo(resultByRef)
            assertThat(resultByRef).isEqualTo(timestamp)
        }
    }

    private fun givenARelation(id: String, aSet: Set<ElementReference<*>>, bSet: Set<ElementReference<*>>): SimpleTestRelation {
        return SimpleTestRelation(id, "x", true, aSet, bSet)
    }

    private fun givenANewerTimestamp(): VectorTimestamp {
        return VectorTimestamp(mutableMapOf(
                "a" to 1L,
                "b" to 2L,
                "c" to 3L
        ))
    }

    private fun givenAnOlderTimestamp(): VectorTimestamp {
        return VectorTimestamp(mutableMapOf(
                "a" to 1L,
                "b" to 1L,
                "c" to 3L
        ))
    }

    private fun givenAnElementReference1(): ElementReference<Element> {
        return ElementReference("test1", Element::class.java)
    }

    private fun givenAnElementReference2(): ElementReference<SimpleTestElement> {
        return ElementReference("test2", SimpleTestElement::class.java)
    }
}
