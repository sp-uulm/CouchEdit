package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp

class MutableTimedDiffCollectionImpl() : MutableTimedDiffCollection, TimedDiffCollectionImpl() {
    constructor(diffs: DiffCollectionImpl) : this(diffs.diffs, emptyMap(), diffs.refreshs)

    private constructor(diffMap: Map<String, ModelDiff>, versions: Map<String, VectorTimestamp>, refreshs: Set<String>) : this() {
        if (!versions.keys.containsAll(diffsInternal.keys)) {
            throw IllegalArgumentException("Versions map must contain Element IDs of all Elements in this Collection")
        }

        this.diffsInternal.putAll(diffMap)
        this.versionsInternal.putAll(versions.filterKeys(diffMap.keys::contains))
        this.refreshsInternal.addAll(refreshs)
    }

    override fun getVersionForElement(id: String) = this.versionsInternal[id] ?: VectorTimestamp()

    override fun filter(predicate: (ModelDiff) -> Boolean): MutableTimedDiffCollection {
        return MutableTimedDiffCollectionImpl(this.diffsInternal.filterValues(predicate), this.versionsInternal, this.refreshsInternal)
    }

    override fun filterByElementTypes(types: List<Class<out Element>>): MutableTimedDiffCollection {
        // This does work because filterByElementTypes in DiffCollectionImpl uses filter() which is overridden here
        return super.filterByElementTypes(types) as MutableTimedDiffCollection
    }

    override fun copy() = MutableTimedDiffCollectionImpl(
            this.diffsInternal.mapValues { (_, value) -> value.copy() },
            this.versions.mapValues { (_, value) -> value.copy() },
            this.refreshs
    )

    /**
     * Adds a [ModelDiff] to this DiffCollection.
     * If a Diff already exists for the given element in the collection, it will be replaced.
     *
     * @param md The ModelDiff to be added.
     */
    override fun putDiff(md: ModelDiff?, affectedTimeStamp: VectorTimestamp) {
        if (md == null) {
            return
        }

        val elementId = md.affected.id
        synchronized(this) {
            this.diffsInternal[elementId] = md
            this.versionsInternal[elementId] = affectedTimeStamp
        }
    }

    override fun setRefresh(id: String) {
        refreshsInternal.add(id)
    }

    /**
     * Merges all Diffs from the given [other] collection
     */
    override fun mergeCollection(other: TimedDiffCollection) {
        synchronized(this) {
            for ((id, diff) in other.diffs) {
                if (this.diffsInternal[id] is ElementRemoveDiff && diff !is ElementRemoveDiff) {
                    this.refreshsInternal.add(id)
                }
                this.diffsInternal[id] = diff
            }

            this.versionsInternal.putAll(other.versions)
            this.refreshsInternal.addAll(other.refreshs)
        }
    }

    override fun mergeNewerFrom(other: TimedDiffCollection) {
        synchronized(this) {
            val versions = this.versions
            val otherVersions = other.versions

            for ((id, diff) in other.diffs) {
                if (this.diffsInternal[id] is ElementRemoveDiff && diff !is ElementRemoveDiff) {
                    this.refreshsInternal.add(id)
                }

                val myVersion = versions[id]
                val otherVersion = otherVersions[id] ?: VectorTimestamp()

                if (myVersion != null) {

                    if (myVersion.relationTo(otherVersion) == VectorTimestamp.CausalRelation.STRICTLY_AFTER) {
                        continue
                    }
                }

                this.diffsInternal[id] = diff
                this.versionsInternal[id] = otherVersion
            }
        }

    }

    override fun toMutable(): MutableTimedDiffCollection = this
}
