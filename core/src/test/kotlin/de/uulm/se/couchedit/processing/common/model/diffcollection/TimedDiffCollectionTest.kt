package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

abstract class TimedDiffCollectionTest : DiffCollectionTest() {
    /**
     * For the tests to work correctly, implementations must set this up so that:
     * * It contains the diffs from [diffList]
     * * The diff contents are timestamped as given by the [versions] map mapping the affected Element's ID to the
     *   corresponding VectorTimestamp.
     */
    abstract override val systemUnderTest: TimedDiffCollection

    val versions = mapOf(
            diff1.affected.id to VectorTimestamp(mutableMapOf("A" to 1L, "B" to 2L, "C" to 3L)),
            diff2.affected.id to VectorTimestamp(mutableMapOf("A" to 1L, "B" to 1L, "C" to 1L)),
            diff3.affected.id to VectorTimestamp(mutableMapOf("A" to 1L, "B" to 3L, "C" to 7L)),
            diff4.affected.id to VectorTimestamp(mutableMapOf("A" to 25L, "B" to 1L, "C" to 2L)),
            diff5.affected.id to VectorTimestamp(mutableMapOf("A" to 2L, "B" to 6L, "C" to 4L))
    )

    @Nested
    open inner class GetVersion {
        @Test
        open fun `should return correct VectorTimestamp for each Element`() {
            for (diff in diffList) {
                val expectedTimestamp = versions[diff.affected.id]


                val containedTimestamp = systemUnderTest.getVersionForElement(diff.affected.id)


                assertThat(containedTimestamp).isEqualTo(expectedTimestamp)
            }
        }
    }

    @Nested
    open inner class Filter : DiffCollectionTest.Filter() {
        @Test
        fun `should also correctly set the VectorTimestamps in the result`() {
            val result = systemUnderTest.filter(predicate)

            assertExistingVersionsSameAsInSource(result, systemUnderTest)
        }
    }

    @Nested
    open inner class FilterByElementTypes : DiffCollectionTest.FilterByElementTypes() {
        @Test
        fun `result should also correctly contain the VectorTimestamps`() {
            val result = systemUnderTest.filterByElementTypes(elementTypes)

            assertExistingVersionsSameAsInSource(result, systemUnderTest)
        }
    }

    @Nested
    open inner class Copy : DiffCollectionTest.Copy() {
        @Test
        fun `should also copy the VectorTimestamps to the result`() {
            val result = systemUnderTest.copy()

            assertExistingVersionsSameAsInSource(result, systemUnderTest)

            for (id in result.diffs.keys) {
                assertThat(result.getVersionForElement(id)).isNotSameAs(systemUnderTest.getVersionForElement(id))
            }

        }
    }


    protected fun assertExistingVersionsSameAsInSource(diffs: TimedDiffCollection, source: TimedDiffCollection) {
        for (id in diffs.diffs.keys) {
            assertThat(diffs.getVersionForElement(id)).describedAs("VectorTimestamp for $id in result was not " +
                    "equal to source DiffCollection").isEqualTo(source.getVersionForElement(id))
        }
    }
}
