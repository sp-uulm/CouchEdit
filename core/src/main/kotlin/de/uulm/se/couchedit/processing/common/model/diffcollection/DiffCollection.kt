package de.uulm.se.couchedit.processing.common.model.diffcollection;

import de.uulm.se.couchedit.model.Copyable
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.ModelDiff

interface DiffCollection : Collection<ModelDiff>, Copyable {
    /**
     * All latest [ModelDiff]s contained in this DiffCollection.
     */
    val diffs: Map<String, ModelDiff>

    /**
     * The Element IDs that should be refreshed
     */
    val refreshs: Set<String>

    /**
     * Deep copies this [DiffCollection], i.e. all the [ModelDiff]s contained will also be copied.
     */
    override fun copy(): DiffCollection

    /**
     * Gets the (latest added) [ModelDiff] for the given Element.
     *
     * @param e The Element for which to retrieve the Diff
     * @return The [ModelDiff] added for this Element, of null if none exists
     *
     * @throws IllegalStateException If the diff stored in the DiffCollection has a type incompatible to that of [ref].
     */
    fun getDiffForElement(ref: ElementReference<*>): ModelDiff?

    fun isRefresh(id: String): Boolean

    /**
     * Returns a new [DiffCollection] containing only the Diffs that match the given [predicate].
     *
     * The DiffCollection Elements should not be copied here; if necessary this needs to be done with the
     * [copy] method afterwards
     */
    fun filter(predicate: (ModelDiff) -> Boolean): DiffCollection

    /**
     * Returns a new [DiffCollection] containing all Diffs that have one of the [Element] types out of [types], or one of its subtypes as their subject.
     *
     * The DiffCollection Elements should not be copied here; if necessary this needs to be done with the
     * [copy] method afterwards.
     */
    fun filterByElementTypes(types: List<Class<out Element>>): DiffCollection

    fun toMutable(): MutableTimedDiffCollection
}
