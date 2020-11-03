package de.uulm.se.couchedit.processing.common.model.time

import de.uulm.se.couchedit.model.Copyable

class VectorTimestamp(private val contents: MutableMap<String, Long> = mutableMapOf()) : MutableMap<String, Long> by contents, Copyable {
    override fun get(key: String): Long {
        return contents[key] ?: 0
    }

    fun relationTo(other: VectorTimestamp): CausalRelation {
        // at least one key in this timestamp is greater than the correspondent one in other
        var oneGreater = false

        // at least one key in this timestamp is less than the correspondent one in other
        var oneLess = false

        for (key in other.contents.keys.union(this.contents.keys)) {
            if (this[key] < other[key]) {
                oneLess = true

                continue
            }

            if (this[key] > other[key]) {
                oneGreater = true
            }
        }

        if (oneGreater && oneLess) {
            return CausalRelation.PARALLEL
        }

        if (oneGreater && !oneLess) {
            return CausalRelation.STRICTLY_AFTER
        }

        if (oneLess && !oneGreater) {
            return CausalRelation.STRICTLY_BEFORE
        }

        return CausalRelation.EQUAL
    }

    override fun copy(): VectorTimestamp {
        return VectorTimestamp(this.contents.toMutableMap())
    }

    override fun equals(other: Any?): Boolean {
        return this.contents == (other as? VectorTimestamp)?.contents
    }

    override fun hashCode(): Int {
        return this.contents.hashCode()
    }

    enum class CausalRelation {
        STRICTLY_BEFORE,
        STRICTLY_AFTER,
        PARALLEL,
        EQUAL
    }
}
