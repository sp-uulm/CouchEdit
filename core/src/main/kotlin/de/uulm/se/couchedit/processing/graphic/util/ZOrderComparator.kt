package de.uulm.se.couchedit.processing.graphic.util

import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import kotlin.math.max

/**
 * Compares the Z coordinates of two [GraphicObject]s.
 *
 * @see GraphicObject.z
 */
object ZOrderComparator : Comparator<GraphicObject<*>> {
    override fun compare(o1: GraphicObject<*>?, o2: GraphicObject<*>?): Int {
        val z1 = o1?.z
        val z2 = o2?.z

        if (z1 == z2) {
            return 0
        }

        if (z1 == null) {
            return -1
        }

        if (z2 == null) {
            return 1
        }

        for (i in 0 until max(z1.size, z2.size)) {
            val o1Val = z1.getOrNull(i) ?: 0
            val o2Val = z2.getOrNull(i) ?: 0

            val cmpResult = o1Val.compareTo(o2Val)

            if (cmpResult == 0) {
                continue
            }

            return cmpResult
        }

        return 0
    }
}
