package de.uulm.se.couchedit.processing.common.testutils.mocks

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject

/**
 * "Mock" implementation of a [ModificationPort] that allows us to:
 *
 * * enqueue arbitrary [TimedDiffCollection]s into the output via the [publishDiffs] method
 * * View what DiffCollections have been received by using the [getAndClearRecordedDiffs] method
 *
 * To effectively use this class, use the [de.uulm.se.couchedit.RxJavaTestCase] so that the receiving of the
 * DiffCollections will run on the same thread as the tests itself and thus assertions can be made.
 */
class TestPort(override val id: String, private val consumes: List<Class<out Element>>) : ModificationPort {
    private val recordedDiffs = mutableListOf<TimedDiffCollection>()

    private val outputSubject = ReplaySubject.create<TimedDiffCollection>()

    /**
     * @return a List of all [TimedDiffCollection]s that have been received by this instance since the last call
     *         to this method
     */
    fun getAndClearRecordedDiffs(): List<TimedDiffCollection> {
        val ret = this.recordedDiffs.toList()

        this.recordedDiffs.clear()

        return ret
    }

    /**
     * Publishes the given [diffCollection] to this Port's connected [output]
     */
    fun publishDiffs(diffCollection: TimedDiffCollection) {
        this.outputSubject.onNext(diffCollection)
    }

    override fun consumes(): List<Class<out Element>> {
        return consumes
    }

    override fun connectInputTo(diffPublisher: Observable<TimedDiffCollection>) {
        diffPublisher.subscribe {
            this.recordedDiffs.add(it)
        }
    }

    override fun getOutput(): Observable<TimedDiffCollection> {
        return outputSubject.hide()
    }
}
