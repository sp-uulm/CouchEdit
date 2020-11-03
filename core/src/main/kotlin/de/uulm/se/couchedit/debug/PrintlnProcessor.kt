package de.uulm.se.couchedit.debug

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollectionImpl

class PrintlnProcessor() : Processor {
    override fun consumes(): List<Class<out Element>> {
        return listOf(Element::class.java)
    }

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        //return TimedDiffCollectionImpl()

        println("diff: -------------" + System.currentTimeMillis().toString() + "-------- DiffCollection $diffs: --------")

        for (diff in diffs.diffs.values) {
            if (diff is ElementAddDiff) {
                println("diff: Element ${diff.added} was added")
            }
            if (diff is ElementRemoveDiff) {
                println("diff: Element ${diff.removed} was removed")
            }
            if (diff is ElementModifyDiff) {
                println("diff: Element ${diff.before} is now ${diff.after}.")
            }
        }

        return TimedDiffCollectionImpl()
    }

}
