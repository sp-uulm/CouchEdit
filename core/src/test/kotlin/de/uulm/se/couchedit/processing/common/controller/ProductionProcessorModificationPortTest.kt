package de.uulm.se.couchedit.processing.common.controller

import de.uulm.se.couchedit.RxJavaTestCase
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollectionImpl
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class ProductionProcessorModificationPortTest : RxJavaTestCase() {
    private val processorMock = TestProcessor(listOf(Element::class.java))

    private val diffCollectionFactoryMock: DiffCollectionFactory = DiffCollectionFactory() // TODO mockk does not seem to work on Processor thread? mockk()

    private val input = PublishSubject.create<TimedDiffCollection>()

    private val observedDiffCollections = mutableSetOf<TimedDiffCollection>()

    private val executor = Executors.newFixedThreadPool(3)

    private val systemUnderTest = ProductionProcessorModificationPort(
            processorMock,
            diffCollectionFactoryMock,
            executor
    )

    @BeforeEach
    fun setUp() {
        systemUnderTest.connectInputTo(input.hide())

        systemUnderTest.getOutput().subscribe {
            observedDiffCollections.add(it)
        }
    }

    @Nested
    inner class Diff_Processing {
        @Test
        fun `should directly call process if it is not blocked`() {
            val id = "test"
            val id2 = "test2"

            val inputDiffCollection = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id)
            val outputDiffCollection = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id2)

            val future = CompletableFuture<Nothing>()

            processorMock.enqueueOperation(outputDiffCollection, future)


            input.onNext(inputDiffCollection)


            // wait on the processing to finish
            future.get()

            val processorObservedDiffs = processorMock.getAndClearRecordedDiffs()

            assertThat(processorObservedDiffs).hasSize(1)

            val firstObservedDiff = processorObservedDiffs.first()

            assertThat(firstObservedDiff).hasSize(1)

            assertThat(firstObservedDiff.diffs[id]).isNotNull
        }

        @Test
        fun `should pass the processing results back into the output`() {
            val id = "test"
            val id2 = "test2"

            val inputDiffCollection = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id)
            val outputDiffCollection = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id2)

            val future = CompletableFuture<Nothing>()

            processorMock.enqueueOperation(outputDiffCollection, future)


            input.onNext(inputDiffCollection)


            // wait on the processing to finish
            future.get()

            // also wait until the executor has finished its run -> passed the result back
            executor.shutdown()
            executor.awaitTermination(20, TimeUnit.SECONDS)


            assertThat(observedDiffCollections).hasSize(1)

            val firstObservedDiffCollection = observedDiffCollections.first()

            assertThat(firstObservedDiffCollection).hasSize(1)
            assertThat(firstObservedDiffCollection.diffs[id2]).isNotNull
        }

        @Test
        fun `should merge diffs with previous as long as the Processor is blocked`() {
            val id1 = "test1"
            val id2 = "test2"
            val id3 = "test3"
            val id4 = "out1"
            val id5 = "out2"

            val inputDiffCollection1 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id1)
            val inputDiffCollection2 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id2)
            val inputDiffCollection3 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id3)
            val outputDiffCollection1 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id4)
            val outputDiffCollection2 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(id5)

            val future1 = CompletableFuture<Nothing>()
            val future2 = CompletableFuture<Nothing>()

            processorMock.enqueueOperation(outputDiffCollection1, future1)
            processorMock.enqueueOperation(outputDiffCollection2, future2)

            processorMock.lockProcessing()

            // this should now block the processing
            input.onNext(inputDiffCollection1)

            sleep(500)

            // these two DiffCollections will have to wait
            input.onNext(inputDiffCollection2)
            input.onNext(inputDiffCollection3)

            sleep(500)

            processorMock.unlockProcessing()


            // wait on the processing to finish
            future2.get()

            val processorObservedDiffs = processorMock.getAndClearRecordedDiffs()

            assertThat(processorObservedDiffs).describedAs("If the Processor was locked, the ModificationPort " +
                    "is supposed to merge the DiffCollections into one big DiffCollection").hasSize(2)

            val firstObservedDiff = processorObservedDiffs[0]
            val secondObservedDiff = processorObservedDiffs[1]

            assertThat(firstObservedDiff).hasSize(1)

            assertThat(firstObservedDiff.diffs[id1]).isNotNull


            assertThat(secondObservedDiff.diffs[id2]).isNotNull
            assertThat(secondObservedDiff.diffs[id3]).isNotNull
        }
    }

    class TestProcessor(private val consumes: List<Class<out Element>>) : Processor {
        private val lock = ReentrantLock()

        private var lockCount = 0

        private val recordedDiffs = mutableListOf<TimedDiffCollection>()

        private val returnQueue = LinkedList<Pair<TimedDiffCollection, CompletableFuture<Nothing>>>()

        /**
         * Enqueues a result for one run of the Processor, along with a [CompletableFuture] that will be completed
         * before the result is used and returned.
         */
        fun enqueueOperation(diffs: TimedDiffCollection, future: CompletableFuture<Nothing>) {
            returnQueue.addLast(Pair(diffs, future))
        }

        fun getAndClearRecordedDiffs(): List<TimedDiffCollection> {
            val ret = this.recordedDiffs.toList()

            this.recordedDiffs.clear()

            return ret
        }

        fun lockProcessing() {
            synchronized(lockCount) {
                this.lock.lock()

                lockCount = 0
            }
        }

        fun unlockProcessing() {
            synchronized(lockCount) {
                this.lock.unlock()

                lockCount = 0
            }
        }

        fun getAndResetLockCount(): Int {
            synchronized(lockCount) {
                val oldCount = lockCount

                lockCount = 0

                return oldCount
            }
        }

        override fun consumes(): List<Class<out Element>> = consumes

        override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
            synchronized(lockCount) {
                lockCount += 1
            }

            try {
                lock.lock()

                recordedDiffs.add(diffs)

                val (nextResult, future) = returnQueue.pop()

                future.complete(null)

                return nextResult
            } finally {
                lock.unlock()
            }
        }
    }

    private fun givenADiffCollectionWithASingleSimpleTestElementAddDiff(elementId: String): TimedDiffCollection {
        val element = SimpleTestElement(elementId, "x")

        val ret = MutableTimedDiffCollectionImpl()

        ret.putDiff(ElementAddDiff(element), VectorTimestamp())

        return ret
    }
}
