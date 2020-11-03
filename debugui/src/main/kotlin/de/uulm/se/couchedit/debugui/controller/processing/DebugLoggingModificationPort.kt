package de.uulm.se.couchedit.debugui.controller.processing

import de.uulm.se.couchedit.debugui.model.processing.DiffCollectionObservation
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.Observable
import javafx.collections.FXCollections
import java.util.*

/**
 * A special [ModificationPort] for debugging purposes that listens on the ModificationBus for any
 * DiffCollections, converts them to [DiffCollectionObservation] objects for displaying purposes and
 * stores these elements in its [observedDiffs] collection.
 */
class DebugLoggingModificationPort : ModificationPort {
    override val id: String = "DEBUG"

    val observedDiffs = FXCollections.observableArrayList<DiffCollectionObservation>()!!

    override fun connectInputTo(diffPublisher: Observable<TimedDiffCollection>) {
        diffPublisher.subscribe {
            val elementAddCount = it.diffs.count { (_, diff) -> diff is ElementAddDiff }
            val elementRemoveCount = it.diffs.count { (_, diff) -> diff is ElementRemoveDiff }
            val elementModifyCount = it.diffs.count { (_, diff) -> diff is ElementModifyDiff }

            val elementTypes = it.diffs.values.map { it.affected::class.java }.toSet()

            val observation = DiffCollectionObservation(
                    Date(),
                    it,
                    elementTypes,
                    it.size,
                    elementAddCount,
                    elementModifyCount,
                    elementRemoveCount
            )

            observedDiffs.add(observation)
        }
    }

    override fun getOutput(): Observable<TimedDiffCollection> = Observable.never()

    override fun consumes(): List<Class<out Element>> = listOf(Element::class.java)
}
