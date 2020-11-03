package de.uulm.se.couchedit.processing.common.services.diff

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepository

/**
 * Delegate for Processors responsible for replaying / applying the effects of [DiffCollection]s
 * onto a [repository]. Handles sequence numbers so that no older changes can overwrite newer ones if they arrive
 * out of order.
 */
@ProcessorScoped
class Applicator @Inject constructor(private val repository: ModelRepository, val diffCollectionFactory: DiffCollectionFactory) {
    enum class ParallelStrategy {
        OVERWRITE, IGNORE
    }

    /**
     * Applies (replays) the diffs in the given [DiffCollection]
     *
     * @param diffs The [DiffCollection] containing [ModelDiff]s to be applied
     * @param parallelStrategy The strategy to apply if an element of the DiffCollection to be applied was modified in
     *                         parallel based on its own vector timestamp.
     * @return DiffCollection representing the changes made to **this** repository following the diffs in the input
     *         Collection.
     */
    fun apply(diffs: DiffCollection, parallelStrategy: ParallelStrategy = ParallelStrategy.IGNORE): TimedDiffCollection {
        synchronized(this) {
            val ret = diffCollectionFactory.createMutableTimedDiffCollection()

            for (diff in diffs.diffs.values) {
                val elementId = diff.affected.id

                val timestamp = (diffs as? TimedDiffCollection)?.getVersionForElement(elementId)

                ret.mergeCollection(this.apply(diff, timestamp, parallelStrategy))

                if (diffs.isRefresh(elementId)) {
                    ret.setRefresh(elementId)
                }
            }

            return ret
        }
    }

    /**
     * Applies (replays) the given [diff] onto the [ModelRepository].
     *
     * @return DiffCollection representing the changes made to **this** repository following the diffs in the input
     *         Collection
     */
    private fun apply(diff: ModelDiff, elementTimestamp: VectorTimestamp?, parallelStrategy: ParallelStrategy): TimedDiffCollection {
        val repoVersion = this.repository.getVersion(diff.affected.id)

        elementTimestamp?.let {
            when (repoVersion.relationTo(it)) {
                VectorTimestamp.CausalRelation.STRICTLY_AFTER ->
                    // If the stored element is newer than the given timestamp, don't overwrite.
                    // if the timestamps are exactly equal, then also don't overwrite
                    return handleIfExplicit(diff, repoVersion)

                VectorTimestamp.CausalRelation.PARALLEL -> {
                    if (parallelStrategy == ParallelStrategy.IGNORE) {
                        return handleIfExplicit(diff, repoVersion)
                    }
                }
                else -> {
                }
            }
        }

        return when (diff) {
            is ElementAddDiff -> repository.store(diff.added, elementTimestamp)
            is ElementModifyDiff -> repository.store(diff.after, elementTimestamp)
            is ElementRemoveDiff -> repository.remove(diff.removed.id, elementTimestamp)
            else -> diffCollectionFactory.createTimedDiffCollection()
        }
    }

    /**
     * Special handling for the case that a timestamp in a DiffCollection to apply is older than the current state
     * but has an [ProbabilityInfo.Explicit] probability. Because Explicit probability always is applied unconditionally,
     * update the internal state accordingly.
     *
     * This method is to be called only if the [diff] is not to be applied principally (because its timestamp is
     * either strictly before or the ParallelStrategy given is IGNORE and the timestamp was parallel).
     * Thus, the current state from the [repository] is taken, the Probability set to [ProbabilityInfo.Explicit] and
     * saved again with the [currentVersion] timestamp (which is the same as already held by the Repository)
     *
     * @todo Generalize this (partial update diffs in general??)
     */
    private fun handleIfExplicit(diff: ModelDiff, currentVersion: VectorTimestamp): TimedDiffCollection {
        if ((diff is ElementAddDiff || diff is ElementModifyDiff) && diff.affected.probability == ProbabilityInfo.Explicit) {
            val currentState = repository[diff.affected.id] ?: return diffCollectionFactory.createTimedDiffCollection()

            if (currentState.probability != ProbabilityInfo.Explicit) {
                currentState.probability = ProbabilityInfo.Explicit

                return repository.store(currentState, currentVersion)
            }
        }

        return diffCollectionFactory.createTimedDiffCollection()
    }
}
