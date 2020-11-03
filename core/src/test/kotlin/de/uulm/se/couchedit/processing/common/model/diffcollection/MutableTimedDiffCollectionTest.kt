package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import de.uulm.se.couchedit.util.extensions.ref
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

abstract class MutableTimedDiffCollectionTest : TimedDiffCollectionTest() {
    abstract override val systemUnderTest: MutableTimedDiffCollection

    @Nested
    open inner class PutDiff {
        @Nested
        open inner class IfNoPreviousDiffExistedForID {
            protected val diffToInsert = givenASimpleOtherTestElementTestModelDiff("insertTest", "test")
            protected val timestampToInsert = VectorTimestamp(mutableMapOf("new1" to 111L, "new2" to 222L))

            /**
             * Common "then" part for these PutDiff tests
             */
            @BeforeEach
            fun then() {
                systemUnderTest.putDiff(diffToInsert, timestampToInsert)
            }

            @Test
            fun `should insert the new Diff into the diffs map if no previous diff did exist`() {
                assertThat(systemUnderTest.diffs[diffToInsert.affected.id]).isEqualTo(diffToInsert)
            }

            @Test
            fun `should return the new Diff in getDiffForElement`() {
                assertThat(systemUnderTest.getDiffForElement(diffToInsert.affected.ref())).isEqualTo(diffToInsert)
            }

            @Test
            fun `should insert the new Timestamp into the versions map`() {
                assertThat(systemUnderTest.versions[diffToInsert.affected.id]).isEqualTo(timestampToInsert)
            }

            @Test
            fun `should return the new Timestamp in getVersionForElement`() {
                assertThat(systemUnderTest.getVersionForElement(diffToInsert.affected.id)).isEqualTo(timestampToInsert)
            }
        }

        @Nested
        open inner class IfPreviousDiffExisted {
            protected val diffToInsert = ElementAddDiff(SimpleTestElement(diff1.affected.id, "abc"))

            @Test
            fun `should replace the existing Diff in the DiffCollection`() {
                val timestamp = versions.getValue(diffToInsert.added.id).copy().also { it["A"] = it["A"] + 1 }


                systemUnderTest.putDiff(diffToInsert, timestamp)


                assertThat(systemUnderTest.getDiffForElement(diffToInsert.added.ref())).isEqualTo(diffToInsert)
            }

            @Test
            fun `should replace the existing timestamp`() {
                val timestamp = versions.getValue(diffToInsert.added.id).copy().also { it["A"] = it["A"] + 1 }


                systemUnderTest.putDiff(diffToInsert, timestamp)


                assertThat(systemUnderTest.getVersionForElement(diffToInsert.added.id)).isEqualTo(timestamp)
            }

            @Test
            fun `should replace the existing timestamp even if it is older than the previous value`() {
                val timestamp = versions.getValue(diffToInsert.added.id).copy().also { it["B"] = it["B"] - 1 }


                systemUnderTest.putDiff(diffToInsert, timestamp)


                assertThat(systemUnderTest.getVersionForElement(diffToInsert.added.id)).isEqualTo(timestamp)
            }
        }
    }

    @Nested
    open inner class MergeCollection : MergeTest() {
        @BeforeEach
        fun then() {
            systemUnderTest.mergeCollection(diffCollectionToBeMerged)
        }

        @Test
        fun `replaces all diffs in the current collection regardless of timestamp`() {
            for (id in diffCollectionToBeMerged.diffs.keys) {
                assertMerged(id)
            }
        }

        @Test
        fun `leaves diffs in the original DiffCollection that dont have a counterpart in the collection to be merged`() {
            assertThat(systemUnderTest.diffs[diff5.affected.id]).isEqualTo(diff5)
            assertThat(systemUnderTest.versions[diff5.affected.id]).isEqualTo(versions[diff5.affected.id])
        }
    }

    @Nested
    open inner class MergeNewerFrom : MergeTest() {
        @BeforeEach
        fun then() {
            systemUnderTest.mergeNewerFrom(diffCollectionToBeMerged)
        }

        @Test
        fun `replaces all diffs in the current collection with equal, newer or parallel timestamp`() {
            for (id in listOf(changedWithNewerTimestamp, changedWithParallelTimestamp, changedWithEqualTimestamp)) {
                assertMerged(id)
            }
        }

        @Test
        fun `does not replace diffs that have timestamp strictly before the current timestamp in the collection to be merged`() {
            assertThat(systemUnderTest.diffs[diff3.affected.id]).isEqualTo(diff3)
            assertThat(systemUnderTest.versions[diff3.affected.id]).isEqualTo(versions[diff3.affected.id])
        }

        @Test
        fun `leaves diffs in the original DiffCollection that dont have a counterpart in the collection to be merged`() {
            assertThat(systemUnderTest.diffs[diff5.affected.id]).isEqualTo(diff5)
            assertThat(systemUnderTest.versions[diff5.affected.id]).isEqualTo(versions[diff5.affected.id])
        }
    }

    /**
     * Common superclass for tests checking behavior when merging one collection into another
     */
    abstract inner class MergeTest {
        /**
         * The Diff for the Element with this ID is different,
         * but the timestamp is the same as in the [systemUnderTest]
         */
        protected val changedWithEqualTimestamp = diff1.affected.id

        /**
         * The Diff for the Element with this ID is different,
         * and the timestamp is newer than in the [systemUnderTest]
         */
        protected val changedWithNewerTimestamp = diff2.affected.id

        /**
         * The Diff for the Element with this ID is different,
         * and the timestamp is older than in the [systemUnderTest]
         */
        protected val changedWithOlderTimestamp = diff3.affected.id

        /**
         * The Diff for the Element with this ID is different,
         * and the timestamp is parallel to that in the [systemUnderTest]
         */
        protected val changedWithParallelTimestamp = diff4.affected.id

        /**
         * The diff for the Element with the given ID is not in the [systemUnderTest],
         * but it is in the [diffCollectionToBeMerged].
         */
        protected val totallyNew = "newInCollectionToBeMerged"

        protected val diffsToBeMerged: Map<String, ModelDiff> = mapOf(
                // This diff has changed, but the timestamp stays the same
                changedWithEqualTimestamp to givenASimpleTestElementTestModelDiff(changedWithEqualTimestamp, "newTest"),
                // This diff has changed with a newer timestamp
                changedWithNewerTimestamp to givenASimpleTestElementTestModelDiff(changedWithNewerTimestamp, "newTest2"),
                // This diff has
                changedWithOlderTimestamp to givenASimpleTestElementTestModelDiff(changedWithOlderTimestamp, "newTest3"),
                changedWithParallelTimestamp to givenASimpleTestElementTestModelDiff(changedWithParallelTimestamp, "newTest4"),
                totallyNew to givenASimpleTestElementTestModelDiff(totallyNew, "totally new")
        )

        protected val versionsToBeMerged: Map<String, VectorTimestamp> = mapOf(
                changedWithEqualTimestamp to versions.getValue(changedWithEqualTimestamp),
                changedWithNewerTimestamp to versions.getValue(changedWithNewerTimestamp).copy().also { it["A"] = it["A"] + 1 },
                changedWithOlderTimestamp to versions.getValue(changedWithOlderTimestamp).copy().also { it["B"] = it["B"] - 1 },
                changedWithParallelTimestamp to versions.getValue(changedWithParallelTimestamp).copy().also {
                    it["B"] = it["B"] + 1
                    it["C"] = it["C"] - 1
                },
                totallyNew to VectorTimestamp(mutableMapOf("A" to 12L))
        )

        val diffCollectionToBeMerged: TimedDiffCollection = TimedDiffCollectionImpl(diffsToBeMerged, versionsToBeMerged, emptySet())

        protected fun assertMerged(id: String) {
            assertThat(systemUnderTest.diffs[id]).describedAs("Merge operation was supposed to merge diff for $id, " +
                    "but did not").isEqualTo(diffCollectionToBeMerged.diffs[id])
            assertThat(systemUnderTest.versions[id]).describedAs("Merge operation was supposed to replace timestamp for $id, " +
                    "but did not").isEqualTo(diffCollectionToBeMerged.versions[id])
        }
    }
}
