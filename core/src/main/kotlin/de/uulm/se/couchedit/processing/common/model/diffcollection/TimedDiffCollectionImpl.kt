package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp

open class TimedDiffCollectionImpl() : TimedDiffCollection, DiffCollectionImpl() {
    protected val versionsInternal = mutableMapOf<String, VectorTimestamp>()

    // This is not particularily clean. However, this is such a frequently visited point in the application that it makes
    // a HUGE performance impact if we convert this to a non-mutable map.
    // As DiffCollections are duplicated among each Processor anyway, it does not too much harm if someone casts it back
    // to a MutableMap.
    override val versions: Map<String, VectorTimestamp> = versionsInternal

    constructor(diffMap: Map<String, ModelDiff>, versions: Map<String, VectorTimestamp>, refreshs: Set<String>) : this() {
        require(versions.keys.containsAll(diffsInternal.keys)) { "Versions map must contain Element IDs of all Elements in this Collection" }

        this.diffsInternal.putAll(diffMap)
        this.versionsInternal.putAll(versions.filterKeys(diffMap.keys::contains))
        this.refreshsInternal.addAll(refreshs)
    }

    override fun getVersionForElement(id: String) = this.versionsInternal[id] ?: VectorTimestamp()

    override fun filter(predicate: (ModelDiff) -> Boolean): TimedDiffCollection {
        return TimedDiffCollectionImpl(this.diffsInternal.filterValues(predicate), this.versionsInternal, this.refreshsInternal)
    }

    override fun filterByElementTypes(types: List<Class<out Element>>): TimedDiffCollection {
        // This does work because filterByElementTypes in DiffCollectionImpl uses filter() which is overridden here
        return super.filterByElementTypes(types) as TimedDiffCollection
    }

    override fun copy(): TimedDiffCollectionImpl {
        return TimedDiffCollectionImpl(
                this.diffsInternal.mapValues { (_, diff) -> diff.copy() },
                this.versionsInternal.mapValues { (_, timestamp) -> timestamp.copy() },
                this.refreshsInternal
        )
    }
}
