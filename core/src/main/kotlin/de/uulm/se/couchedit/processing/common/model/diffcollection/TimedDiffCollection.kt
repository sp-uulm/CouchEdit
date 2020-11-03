package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp

/**
 * A [DiffCollection] which carries a map of Vector Timestamps for all Elements for which diffs are included.
 * The vector timestamps represent the version **after** the specified [ModelDiff] has taken place.
 */
interface TimedDiffCollection : DiffCollection {

    val versions: Map<String, VectorTimestamp>

    /**
     * Vector timestamp that has been given to the element with the given [id] in the source diff collection.
     */
    fun getVersionForElement(id: String): VectorTimestamp

    override fun filter(predicate: (ModelDiff) -> Boolean): TimedDiffCollection

    override fun filterByElementTypes(types: List<Class<out Element>>): TimedDiffCollection

    override fun copy(): TimedDiffCollection
}
