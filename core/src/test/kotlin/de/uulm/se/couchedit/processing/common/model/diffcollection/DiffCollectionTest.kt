package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.testutils.model.OtherSimpleTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestRelation
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

abstract class DiffCollectionTest {
    /**
     * Subclasses must implement this value containing the diffs from [diffList] for the
     * test cases to run correctly.
     */
    protected abstract val systemUnderTest: DiffCollection

    protected val diff1 = givenASimpleTestElementTestModelDiff("01", "test")
    protected val diff2 = givenASimpleTestElementTestModelDiff("02", "test2")
    protected val diff3 = givenASimpleTestElementTestModelDiff("03", "taest")
    protected val diff4 = givenASimpleOtherTestElementTestModelDiff("04", "test4")
    protected val diff5 = givenASimpleTestRelationTestModelDiff("05", "test5")

    protected val diffList = listOf(diff1, diff2, diff3, diff4, diff5)

    @Nested
    open inner class Copy {
        @Test
        open fun `should return a different DiffCollection instance`() {
            val result = systemUnderTest.copy()

            assertThat(result).isNotSameAs(systemUnderTest)
        }

        @Test
        open fun `should contain copies of all contained Diffs in the return value`() {
            val result = systemUnderTest.copy()

            for ((id, diff) in systemUnderTest.diffs) {
                val resultDiff = result.diffs[id] as TestModelDiff

                assertThat(resultDiff.original).isSameAs(diff)
            }
        }
    }

    @Nested
    open inner class GetDiffForElement {
        @Test
        open fun `should return exact instance of the diff stored by the DiffCollection`() {
            for (diff in diffList) {
                val modelDiff = systemUnderTest.getDiffForElement(diff.affected.ref())

                assertThat(modelDiff).isSameAs(diff)
            }
        }

        @Test
        open fun `should throw IllegalStateException if the type of Element in the DiffCollection is incompatible with the reference type`() {
            val ref = ElementReference(diff1.affected.id, OtherSimpleTestElement::class.java)

            assertThatIllegalStateException().isThrownBy {
                systemUnderTest.getDiffForElement(ref)
            }
        }
    }

    @Nested
    open inner class Filter {
        protected val predicate = fun(modelDiff: ModelDiff): Boolean {
            val content = when (val affected = modelDiff.affected) {
                is SimpleTestElement -> affected.x
                is SimpleTestRelation -> affected.x
                is OtherSimpleTestElement -> affected.y
                else -> ""
            }

            return content.startsWith("test")
        }

        @Test
        open fun `should return new DiffCollection with the exact instance of the diffs stored by the DiffCollection matching the predicate`() {
            val result = systemUnderTest.filter(predicate)


            assertDiffMapContains(
                    result.diffs,
                    mustContainDiffs = listOf(diff1, diff2, diff4, diff5),
                    mustNotContainDiffs = listOf(diff3)
            )
        }

        @Test
        open fun `should not affect the DiffCollection itself`() {
            val diffsBefore = systemUnderTest.diffs.toMap()


            systemUnderTest.filter(predicate)


            val diffsAfter = systemUnderTest.diffs.toMap()

            assertThat(diffsBefore).isEqualTo(diffsAfter)
        }
    }

    @Nested
    open inner class FilterByElementTypes {
        protected val elementTypes = listOf(SimpleTestElement::class.java, SimpleTestRelation::class.java)

        @Test
        open fun `should return new DiffCollection with the exact instance of the diffs stored by the DiffCollection containing Elements of any of the types`() {
            val result = systemUnderTest.filterByElementTypes(elementTypes)


            assertDiffMapContains(
                    result.diffs,
                    mustContainDiffs = listOf(diff1, diff2, diff3, diff5),
                    mustNotContainDiffs = listOf(diff4)
            )
        }

        @Test
        open fun `should not affect the DiffCollection itself`() {
            val diffsBefore = systemUnderTest.diffs.toMap()


            systemUnderTest.filterByElementTypes(elementTypes)


            val diffsAfter = systemUnderTest.diffs.toMap()

            assertThat(diffsBefore).isEqualTo(diffsAfter)
        }
    }

    @Nested
    open inner class ToMutable {
        @Test
        fun `should return implementation of MutableTimedDiffCollection containing an exact instance of all Diffs in the Collection`() {
            val result = systemUnderTest.toMutable()


            assertDiffMapContains(
                    result.diffs,
                    mustContainDiffs = diffList
            )
        }
    }

    protected fun givenASimpleTestElementTestModelDiff(id: String, content: String): TestModelDiff {
        val element = SimpleTestElement(id, content)

        return TestModelDiff(element)
    }

    protected fun givenASimpleOtherTestElementTestModelDiff(id: String, content: String): TestModelDiff {
        val element = OtherSimpleTestElement(id, content)

        return TestModelDiff(element)
    }

    protected fun givenASimpleTestRelationTestModelDiff(id: String, content: String): TestModelDiff {
        val element = SimpleTestRelation(
                id,
                content,
                true,
                setOf(ElementReference("a", Element::class.java)),
                setOf(ElementReference("b", Element::class.java))
        )

        return TestModelDiff(element)
    }

    protected fun assertDiffMapContains(
            map: Map<String, ModelDiff>,
            mustContainDiffs: List<ModelDiff> = emptyList(),
            mustNotContainDiffs: List<ModelDiff> = emptyList()
    ) {
        for (diff in mustContainDiffs) {
            assertThat(map[diff.affected.id]).describedAs(
                    "DiffCollection must contain diff for ${diff.affected.ref()}"
            ).isSameAs(diff)
        }

        for (diff in mustNotContainDiffs) {
            assertThat(map[diff.affected.id]).describedAs(
                    "DiffCollection must not contain diff for ${diff.affected.ref()}"
            ).isNull()
        }
    }

    /**
     * Special [ModelDiff] instance for testing. Lets us easily identify copied instances as they
     * keep a reference of their original.
     */
    class TestModelDiff(override val affected: Element) : ModelDiff() {
        var original: TestModelDiff? = null

        override fun copy(): ModelDiff {
            return TestModelDiff(this.affected.copy()).also { it.original = this }
        }
    }
}
