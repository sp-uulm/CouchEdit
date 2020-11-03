package de.uulm.se.couchedit.processing.common.services.diff

import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleSubclassTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import de.uulm.se.couchedit.util.extensions.ref
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ApplicatorTest {
    val modelRepositoryMock: ModelRepository = mockk()

    val diffCollectionFactoryMock: DiffCollectionFactory = mockk()

    val systemUnderTest = Applicator(modelRepositoryMock, diffCollectionFactoryMock)

    /**
     * the last MutableTimedDiffCollection returned by the [diffCollectionFactoryMock]
     */
    lateinit var mutableTimedDiffCollectionMock: MutableTimedDiffCollection

    @BeforeEach
    fun setUp() {
        val initializeDiffCollectionMock = fun(): MutableTimedDiffCollection {
            mutableTimedDiffCollectionMock = mockk(relaxUnitFun = true)

            return mutableTimedDiffCollectionMock
        }

        every { diffCollectionFactoryMock.createMutableTimedDiffCollection() } answers { initializeDiffCollectionMock() }
        every { diffCollectionFactoryMock.createTimedDiffCollection() } answers { initializeDiffCollectionMock() }

        every { modelRepositoryMock.getVersion(any()) } answers {
            VectorTimestamp()
        }
    }

    @Nested
    inner class Apply {
        lateinit var element1: SimpleTestElement
        lateinit var element2: SimpleSubclassTestElement

        @BeforeEach
        fun setUp() {
            element1 = givenASimpleTestElement("1")
            element2 = givenASimpleSubclassTestElement("2")
        }

        @Test
        fun `should store all Elements of ElementAddDiffs and merge the resulting DiffCollections to its output`() {
            val diffCollection = givenATimedDiffCollection(
                    mapOf(element1.id to ElementAddDiff(element1), element2.id to ElementAddDiff(element2)),
                    mapOf(element1.id to givenATimestamp(), element2.id to givenATimestamp())
            )

            val resultingDiffCollection1 = givenATimedDiffCollection(
                    mapOf(element1.id to ElementAddDiff(element1)),
                    mapOf(element1.id to givenATimestamp())
            )

            val resultingDiffCollection2 = givenATimedDiffCollection(
                    mapOf(element2.id to ElementAddDiff(element2)),
                    mapOf(element2.id to givenATimestamp())
            )

            every { modelRepositoryMock.store(eq(element1), eq(givenATimestamp())) } returns resultingDiffCollection1
            every { modelRepositoryMock.store(eq(element2), eq(givenATimestamp())) } returns resultingDiffCollection2


            // returns a mock as given by the diffCollectionFactoryMock
            val result = systemUnderTest.apply(diffCollection)


            assertThat(result).isSameAs(mutableTimedDiffCollectionMock)

            verify {
                modelRepositoryMock.store(eq(element1), eq(givenATimestamp()))
                modelRepositoryMock.store(eq(element2), eq(givenATimestamp()))
            }

            result as MutableTimedDiffCollection
            verify {
                result.mergeCollection(resultingDiffCollection1)
                result.mergeCollection(resultingDiffCollection2)
            }
        }

        @Test
        fun `should store all Elements of ElementModifyDiffs and merge the resulting DiffCollections to its output`() {
            val oldElement1 = element1.copy().apply { x = "bla" }
            val oldElement2 = element2.copy().apply { x = "fasel" }

            val diffCollection = givenATimedDiffCollection(
                    mapOf(
                            element1.id to ElementModifyDiff(oldElement1, element1),
                            element2.id to ElementModifyDiff(oldElement2, element2)
                    ),
                    mapOf(element1.id to givenATimestamp(), element2.id to givenATimestamp())
            )

            val resultingDiffCollection1 = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, element1)),
                    mapOf(element1.id to givenATimestamp())
            )

            val resultingDiffCollection2 = givenATimedDiffCollection(
                    mapOf(element2.id to ElementModifyDiff(oldElement2, element2)),
                    mapOf(element2.id to givenATimestamp())
            )

            every { modelRepositoryMock.store(eq(element1), eq(givenATimestamp())) } returns resultingDiffCollection1
            every { modelRepositoryMock.store(eq(element2), eq(givenATimestamp())) } returns resultingDiffCollection2


            // returns a mock as given by the diffCollectionFactoryMock
            val result = systemUnderTest.apply(diffCollection)


            assertThat(result).isSameAs(mutableTimedDiffCollectionMock)

            verify {
                modelRepositoryMock.store(eq(element1), eq(givenATimestamp()))
                modelRepositoryMock.store(eq(element2), eq(givenATimestamp()))
            }

            result as MutableTimedDiffCollection
            verify {
                result.mergeCollection(resultingDiffCollection1)
                result.mergeCollection(resultingDiffCollection2)
            }
        }

        @Test
        fun `should remove all Elements of ElementRemoveDiffs and merge the resulting DiffCollections to its output`() {
            val diffCollection = givenATimedDiffCollection(
                    mapOf(element1.id to ElementRemoveDiff(element1), element2.id to ElementRemoveDiff(element2)),
                    mapOf(element1.id to givenATimestamp(), element2.id to givenATimestamp())
            )

            val resultingDiffCollection1 = givenATimedDiffCollection(
                    mapOf(element1.id to ElementRemoveDiff(element1)),
                    mapOf(element1.id to givenATimestamp())
            )

            val resultingDiffCollection2 = givenATimedDiffCollection(
                    mapOf(element2.id to ElementRemoveDiff(element2)),
                    mapOf(element2.id to givenATimestamp())
            )

            every { modelRepositoryMock.remove(eq(element1.id), eq(givenATimestamp())) } returns resultingDiffCollection1
            every { modelRepositoryMock.remove(eq(element2.id), eq(givenATimestamp())) } returns resultingDiffCollection2


            // returns a mock as given by the diffCollectionFactoryMock
            val result = systemUnderTest.apply(diffCollection)


            assertThat(result).isSameAs(mutableTimedDiffCollectionMock)

            verify {
                modelRepositoryMock.remove(eq(element1.id), eq(givenATimestamp()))
                modelRepositoryMock.remove(eq(element2.id), eq(givenATimestamp()))
            }

            result as MutableTimedDiffCollection
            verify {
                result.mergeCollection(resultingDiffCollection1)
                result.mergeCollection(resultingDiffCollection2)
            }
        }

        @Test
        fun `should not apply diffs with timestamps older than the repo version`() {
            val oldElement1 = element1.copy().apply { x = "bla" }

            every { modelRepositoryMock.getVersion(element1.id) } returns givenATimestamp()

            val diffCollection = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, element1)),
                    mapOf(element1.id to givenAnOlderTimeStamp())
            )

            systemUnderTest.apply(diffCollection, Applicator.ParallelStrategy.IGNORE)

            verify(inverse = true) {
                modelRepositoryMock.store(any(), any())
                mutableTimedDiffCollectionMock.mergeCollection(any())
            }
        }

        @Test
        fun `should not apply diffs with timestamps parallel to the repo version if ParallelStrategy_IGNORE is given`() {
            val oldElement1 = element1.copy().apply { x = "bla" }

            every { modelRepositoryMock.getVersion(element1.id) } returns givenATimestamp()

            val diffCollection = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, element1)),
                    mapOf(element1.id to givenAParallelTimeStamp())
            )

            systemUnderTest.apply(diffCollection, Applicator.ParallelStrategy.IGNORE)

            verify(inverse = true) {
                modelRepositoryMock.store(any(), any())
                mutableTimedDiffCollectionMock.mergeCollection(any())
            }
        }

        @Test
        fun `should store diffs with timestamps parallel to the repo version if ParallelStrategy_OVERWRITE is given`() {
            val oldElement1 = element1.copy().apply { x = "bla" }

            every { modelRepositoryMock.getVersion(element1.id) } returns givenATimestamp()

            val diffCollection = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, element1)),
                    mapOf(element1.id to givenAParallelTimeStamp())
            )

            val resultingDiffCollection1 = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, element1)),
                    mapOf(element1.id to givenAParallelTimeStamp())
            )

            every { modelRepositoryMock.store(eq(element1), eq(givenAParallelTimeStamp())) } returns resultingDiffCollection1


            val result = systemUnderTest.apply(diffCollection, Applicator.ParallelStrategy.OVERWRITE)


            verify {
                modelRepositoryMock.store(eq(element1), eq(givenAParallelTimeStamp()))
            }

            result as MutableTimedDiffCollection
            verify {
                result.mergeCollection(resultingDiffCollection1)
            }
        }

        @Test
        fun `should store Explicit probability to the repository without changing their timestamp even for older timestamps`() {
            // the ModelRepository contains the oldElement1 state timestamped with element1RepoTimestamp.
            val oldElement1 = element1.copy().apply {
                x = "bla"
                probability = ProbabilityInfo.Generated(0.9)
            }

            val element1RepoTimestamp = givenATimestamp()

            every { modelRepositoryMock.getVersion(element1.id) } returns element1RepoTimestamp
            every { modelRepositoryMock[element1.id] } returns oldElement1
            every { modelRepositoryMock[element1.ref()] } returns oldElement1

            // it is expected that the Applicator writes the current repository state, but with the Explicit probability
            val expectedStoredElement = oldElement1.copy().apply {
                probability = ProbabilityInfo.Explicit
            }

            /*
             * Storing the "old" state with explicit probability and the current timestamp
             * will return a modify diff from generated to explicit probability
             */
            val resultingDiffCollection1 = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, expectedStoredElement)),
                    mapOf(element1.id to element1RepoTimestamp)
            )

            every {
                modelRepositoryMock.store(match {
                    it.equivalent(expectedStoredElement)
                }, element1RepoTimestamp)
            } returns resultingDiffCollection1

            element1.probability = ProbabilityInfo.Explicit

            val inputDiffCollection = givenATimedDiffCollection(
                    mapOf(element1.id to ElementModifyDiff(oldElement1, element1)),
                    mapOf(element1.id to givenAnOlderTimeStamp())
            )

            systemUnderTest.apply(inputDiffCollection, Applicator.ParallelStrategy.IGNORE)


            verify {
                modelRepositoryMock.store(match { it.equivalent(expectedStoredElement) }, eq(element1RepoTimestamp))
                mutableTimedDiffCollectionMock.mergeCollection(eq(resultingDiffCollection1))
            }
        }
    }

    private fun givenATimestamp(): VectorTimestamp {
        return VectorTimestamp(mutableMapOf(
                "a" to 1L,
                "b" to 2L,
                "c" to 3L
        ))
    }

    private fun givenAnOlderTimeStamp(): VectorTimestamp {
        return VectorTimestamp(mutableMapOf(
                "b" to 1L,
                "c" to 1L
        ))
    }

    private fun givenAParallelTimeStamp(): VectorTimestamp {
        return VectorTimestamp(mutableMapOf(
                "a" to 42L,
                "b" to 1L,
                "c" to 1L
        ))
    }

    private fun givenASimpleTestElement(id: String): SimpleTestElement {
        return SimpleTestElement(id, "abc")
    }

    private fun givenASimpleSubclassTestElement(id: String): SimpleSubclassTestElement {
        return SimpleSubclassTestElement(id, "def", "ghi")
    }

    private fun givenATimedDiffCollection(
            diffs: Map<String, ModelDiff>,
            versions: Map<String, VectorTimestamp>
    ): TimedDiffCollection {
        val ret = mockk<TimedDiffCollection>(relaxUnitFun = true)

        every { ret.diffs } returns diffs
        every { ret.versions } returns versions
        every { ret.getVersionForElement(any()) } answers {
            versions[firstArg()] ?: VectorTimestamp()
        }
        every { ret.isRefresh(any()) } returns (false)

        return ret
    }
}
