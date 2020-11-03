package de.uulm.se.couchedit.processing.common.modbus

import de.uulm.se.couchedit.RxJavaTestCase
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollectionImpl
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.testutils.mocks.TestPort
import de.uulm.se.couchedit.processing.common.testutils.model.OtherSimpleTestElement
import de.uulm.se.couchedit.processing.common.testutils.model.SimpleTestElement
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Observer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ModificationBusManagerTest : RxJavaTestCase() {
    lateinit var systemUnderTest: ModificationBusManager

    // Class name with underscores to emphasise that this is not a test of a function but of a
    // behavior
    @Suppress("ClassName")
    @Nested
    inner class Incoming_Diff_Processing {
        private val port1 = TestPort("port1", listOf(SimpleTestElement::class.java))
        private val port2 = TestPort("port2", listOf(OtherSimpleTestElement::class.java))

        private val stateCache = PassthroughModificationBusStateCache()

        @BeforeEach
        fun setUp() {
            systemUnderTest = ModificationBusManager(setOf(port1, port2), stateCache)
        }

        @Test
        fun `should pass incoming DiffCollections to all ModPorts, filtered by the consumes method of each port`() {
            val element1Id = "test1"
            val element2Id = "test2"

            val dc1 = givenADiffCollectionWithASingleSimpleTestElementAddDiff(element1Id)
            val dc2 = givenADiffCollectionWithASingleOtherTestElementAddDiff(element2Id)


            port1.publishDiffs(dc1)
            port1.publishDiffs(dc2)


            val port1Diffs = port1.getAndClearRecordedDiffs()
            val port2Diffs = port2.getAndClearRecordedDiffs()

            assertThat(port1Diffs).describedAs("port1 should have received only one DiffCollection " +
                    "as the second enqueued one did not contain an Element type that port1 consumes").hasSize(1)
            assertThat(port2Diffs).describedAs("port2 should have received only one DiffCollection " +
                    "as the first enqueued one did not contain an Element type that port2 consumes").hasSize(1)

            val port1DiffCollection = port1Diffs.first()
            val port2DiffCollection = port2Diffs.first()

            assertThat(port1DiffCollection.diffs[element1Id]).isNotNull
            assertThat(port2DiffCollection.diffs[element2Id]).isNotNull
        }

        @Test
        fun `should pass incoming DiffCollections to the StateCache`() {
            val elementId = "test"

            val diffCollection = givenADiffCollectionWithASingleSimpleTestElementAddDiff(elementId)


            // enqueue the DiffCollection into the ModificationBusManager
            port1.publishDiffs(diffCollection)


            val stateCacheDiffs = stateCache.getAndClearRecordedDiffs()

            assertThat(stateCacheDiffs.first().diffs[elementId]).isInstanceOf(ElementAddDiff::class.java)
        }
    }

    @Nested
    inner class RegisterModificationPort {
        private val stateCache = mockk<ModificationBusStateCache>(relaxed = true, relaxUnitFun = true)

        @BeforeEach
        fun setUp() {
            systemUnderTest = ModificationBusManager(setOf(), stateCache)
        }

        @Test
        fun `should pass the registration of the port on to the cache`() {
            val port = TestPort("xyz", listOf(Element::class.java))


            systemUnderTest.registerModificationPort(port)


            verify { stateCache.attach(eq(port), any()) }
        }
    }

    class PassthroughModificationBusStateCache : ModificationBusStateCache {
        private val recordedDiffs = mutableListOf<TimedDiffCollection>()

        fun getAndClearRecordedDiffs(): List<TimedDiffCollection> {
            val ret = this.recordedDiffs.toList()

            this.recordedDiffs.clear()

            return ret
        }

        override fun connect(input: Flowable<TimedDiffCollection>, output: Observer<TimedDiffCollection>) {
            input.doOnNext { recordedDiffs.add(it) }.subscribe { output.onNext(it) }
        }

        override fun attach(modificationPort: ModificationPort, observable: Observable<TimedDiffCollection>) {
            modificationPort.connectInputTo(observable)
        }

        override fun dump(): TimedDiffCollection {
            throw NotImplementedError()
        }

    }

    private fun givenADiffCollectionWithASingleSimpleTestElementAddDiff(elementId: String): TimedDiffCollection {
        val element = SimpleTestElement(elementId, "x")

        val ret = MutableTimedDiffCollectionImpl()

        ret.putDiff(ElementAddDiff(element), VectorTimestamp())

        return ret
    }

    private fun givenADiffCollectionWithASingleOtherTestElementAddDiff(elementId: String): TimedDiffCollection {
        val element = OtherSimpleTestElement(elementId, "x")

        val ret = MutableTimedDiffCollectionImpl()

        ret.putDiff(ElementAddDiff(element), VectorTimestamp())

        return ret
    }
}
