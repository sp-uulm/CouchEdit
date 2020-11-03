package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp

interface MutableTimedDiffCollection : TimedDiffCollection {
    /**
     * Adds a [ModelDiff] to this DiffCollection.
     * If a Diff already exists for the given element in the collection, it will be replaced.
     *
     * @param md The ModelDiff to be added.
     * @param affectedTimeStamp The time stamp given to the element in the ModelDiff after the operation described by
     *                          the ModelDiff has been completed
     */
    fun putDiff(md: ModelDiff?, affectedTimeStamp: VectorTimestamp)

    fun setRefresh(id: String)

    /**
     * Merges all Diffs from the given [other] collection into this Collection. If an element is included in both this
     * and the [other] DiffCollection, then the elements of this DiffCollection are overwritten with those of the new one.
     */
    fun mergeCollection(other: TimedDiffCollection)

    /**
     * Merges the Diffs from the given [other] DiffCollection into this DiffCollection. Only those Diffs are merged for
     * which the TimeStamp of [other] is newer than or equal to that of this.
     */
    fun mergeNewerFrom(other: TimedDiffCollection)

    /**
     * Deep copies this [DiffCollection], i.e. all the [ModelDiff]s contained will also be copied.
     */
    override fun copy(): MutableTimedDiffCollection
}
