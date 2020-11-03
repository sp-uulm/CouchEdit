package de.uulm.se.couchedit.processing.common.testutils.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo

open class SimpleTestElement(override val id: String, var x: String) : Element {
    override var probability: ProbabilityInfo? = null

    override fun copy() = SimpleTestElement(id, x).also {
        it.probability = probability
    }

    override fun contentEquivalent(other: Any?): Boolean {
        return this.x == (other as? SimpleTestElement)?.x
    }
}
