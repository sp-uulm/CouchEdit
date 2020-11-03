package de.uulm.se.couchedit.processing.common.modbus

import de.uulm.se.couchedit.RxJavaTestCase
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollectionImpl
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.testutils.mocks.TestPort
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ModificationBusStateCacheImplTest : RxJavaTestCase() {
    val modelRepositoryMock: ModelRepository = mockk()

    val applicatorMock: Applicator = mockk(relaxUnitFun = true, relaxed = true)

    val systemUnderTest = ModificationBusStateCacheImpl(modelRepositoryMock, applicatorMock)

    val input = PublishSubject.create<TimedDiffCollection>()

    val observedDiffCollections = mutableSetOf<TimedDiffCollection>()

    @BeforeEach
    fun setUp() {
        systemUnderTest.connect(
                input.toFlowable(BackpressureStrategy.BUFFER).hide(),
                object : Observer<TimedDiffCollection> {
                    override fun onComplete() {}
                    override fun onSubscribe(d: Disposable) {}
                    override fun onError(e: Throwable) {}

                    override fun onNext(t: TimedDiffCollection) {
                        observedDiffCollections.add(t)
                    }
                }
        )
    }

    // Class name with underscores to emphasise that this is not a test of a function but of a
    // behavior
    @Suppress("ClassName")
    @Nested
    inner class Incoming_Diff_Processing {
        @Test
        fun `must pass on incoming DiffCollections to the output after inserting them to the cache`() {
            val elementId = "test"
            val diffCollection = givenADiffCollectionWithASingleSimpleTestElementAddDiff(elementId)


            input.onNext(diffCollection)


            assertThat(observedDiffCollections).hasSize(1)

            verify { applicatorMock.apply(eq(diffCollection), any()) }
        }
    }

    @Nested
    inner class Attach {
        @Test
        fun `must enqueue the dumped ModelRepository state first`() {
            val cachedElementId1 = "test1"
            val cachedElementId2 = "test2"
            val cachedElementId3 = "test3"

            val diffs1 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(cachedElementId1)
            val diffs2 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(cachedElementId2)
            val diffs3 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(cachedElementId3)

            every { modelRepositoryMock.dump() } returns diffs1

            val followingObservable = Observable.just(diffs2, diffs3)

            val port = TestPort("port", listOf(Element::class.java))


            systemUnderTest.attach(port, followingObservable)


            val observedDiffs = port.getAndClearRecordedDiffs()

            val firstDiff = observedDiffs[0]
            val secondDiff = observedDiffs[1]
            val thirdDiff = observedDiffs[2]

            assertThat(firstDiff.diffs[cachedElementId1]).isNotNull
            assertThat(secondDiff.diffs[cachedElementId2]).isNotNull
            assertThat(thirdDiff.diffs[cachedElementId3]).isNotNull
        }
    }

    private fun givenADiffCollectionWithASingleSimpleTestElementAddDiff(elementId: String): TimedDiffCollection {
        val element = SimpleTestElement(elementId, "x")

        val ret = MutableTimedDiffCollectionImpl()

        ret.putDiff(ElementAddDiff(element), VectorTimestamp())

        return ret
    }
}
