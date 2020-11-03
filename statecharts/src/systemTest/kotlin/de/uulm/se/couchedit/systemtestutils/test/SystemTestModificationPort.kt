package de.uulm.se.couchedit.systemtestutils.test

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject

/**
 * [ModificationPort] implementation that enables system tests to publish arbitrary [TimedDiffCollection]s.
 */
class SystemTestModificationPort : ModificationPort {
    override val id: String = "SystemTestModificationPort"

    private val outputSubject = ReplaySubject.create<TimedDiffCollection>().toSerialized()

    override fun consumes(): List<Class<out Element>> = emptyList()

    override fun connectInputTo(diffPublisher: Observable<TimedDiffCollection>) {}

    override fun getOutput(): Observable<TimedDiffCollection> {
        return outputSubject.hide()
    }

    fun publish(timedDiffCollection: TimedDiffCollection) {
        outputSubject.onNext(timedDiffCollection)
    }
}
