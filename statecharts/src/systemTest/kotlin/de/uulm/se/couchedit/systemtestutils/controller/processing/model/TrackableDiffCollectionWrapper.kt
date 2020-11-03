package de.uulm.se.couchedit.systemtestutils.controller.processing.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection

/**
 * Test utility that decorates a [TimedDiffCollection] with an unique set of IDs so that we can track where that DiffCollection's
 * Diffs have been received/processed.
 *
 * The [ids] of the Wrapper are preserved even when copying or filtering the DiffCollection's results.
 */
class TrackableDiffCollectionWrapper(
        val ids: Set<String>,
        val diffCollection: TimedDiffCollection
) : TimedDiffCollection by diffCollection {
    override fun filter(predicate: (ModelDiff) -> Boolean): TrackableDiffCollectionWrapper {
        return wrap(this.diffCollection.filter(predicate))
    }

    override fun filterByElementTypes(types: List<Class<out Element>>): TrackableDiffCollectionWrapper {
        return wrap(this.diffCollection.filterByElementTypes(types))
    }

    override fun copy(): TrackableDiffCollectionWrapper {
        return wrap(diffCollection.copy())
    }

    private fun wrap(diffCollection: TimedDiffCollection): TrackableDiffCollectionWrapper {
        return TrackableDiffCollectionWrapper(this.ids.toSet(), diffCollection)
    }

    override fun toString(): String {
        return "DCW $ids"
    }
}
