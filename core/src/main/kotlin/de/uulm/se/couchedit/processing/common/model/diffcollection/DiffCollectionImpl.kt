package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.ModelDiff

/**
 * A DiffCollection is a special [Collection] of [ModelDiff]s, which apart from the usual methods takes into account
 * the IDs and classes of the [Element]s contained in it.
 *
 * The [DiffCollection] always returns the latest [ModelDiff] applying to a certain [Element] ID added to it.
 */
open class DiffCollectionImpl() : DiffCollection {
    /**
     * Mapping from [Element ID] to the Diff which applies to this [Element]
     */
    protected var diffsInternal: MutableMap<String, ModelDiff> = mutableMapOf()

    protected var refreshsInternal: MutableSet<String> = mutableSetOf()

    override val refreshs: Set<String>
        get() = refreshsInternal

    /**
     * All latest [ModelDiff]s contained in this DiffCollection.
     */
    // This is not particularily clean. However, this is such a frequently visited point in the application that it makes
    // a HUGE performance impact if we convert this to a non-mutable map.
    // As DiffCollections are duplicated among each Processor anyway, it does not too much harm if someone casts it back
    // to a MutableMap.
    override val diffs: Map<String, ModelDiff>
        get() = diffsInternal

    /**
     * Attention: this size is not necessarily the number of [putDiff] calls made. If multiple inserted [ModelDiff]s
     * have the same element as their subject, they only count as one.
     */
    override val size: Int
        get() = this.diffsInternal.size

    internal constructor(diffs: DiffCollection) : this(diffs.diffs, emptySet())

    protected constructor(diffs: DiffCollectionImpl) : this(diffs.diffsInternal, diffs.refreshsInternal)

    constructor(diffMap: Map<String, ModelDiff>, refreshs: Set<String> = emptySet()) : this() {
        diffsInternal = diffMap.toMutableMap()
        this.refreshsInternal = refreshs.toMutableSet()
    }

    override fun isRefresh(id: String): Boolean {
        return id in refreshsInternal
    }

    override fun filterByElementTypes(types: List<Class<out Element>>): DiffCollection {
        return this.filter { diff -> types.find { it.isAssignableFrom(diff.affected.javaClass) } != null }
    }

    override fun filter(predicate: (ModelDiff) -> Boolean): DiffCollection {
        return DiffCollectionImpl(this.diffsInternal.filterValues(predicate), this.refreshsInternal)
    }

    override fun getDiffForElement(ref: ElementReference<*>): ModelDiff? {
        val diff = this.diffsInternal[ref.id] ?: return null

        val elementClass = diff.affected::class.java

        if (!ref.referencesType(elementClass)) {
            throw IllegalStateException("ElementReference $ref references incompatible type to actual " +
                    "Element type ${elementClass.simpleName}")
        }

        return diff
    }

    override fun copy() = DiffCollectionImpl(this.diffsInternal.mapValues { (_, value) -> value.copy() }, refreshsInternal)

    override fun toMutable(): MutableTimedDiffCollection = MutableTimedDiffCollectionImpl(this)

    /* COLLECTION METHODS */

    override fun isEmpty(): Boolean = this.diffsInternal.isEmpty()

    override fun contains(element: ModelDiff): Boolean = this.diffsInternal.containsValue(element)

    override fun containsAll(elements: Collection<ModelDiff>): Boolean = elements.all(this.diffsInternal::containsValue)

    override fun iterator(): Iterator<ModelDiff> = this.diffsInternal.values.iterator()
}
