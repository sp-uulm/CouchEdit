package de.uulm.se.couchedit.processing.common.testutils.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo

/**
 * An Element implementation for testing without connection to [SimpleTestElement]
 */
class OtherSimpleTestElement(override val id: String, val y: String) : Element {
    override var probability: ProbabilityInfo? = null

    override fun copy() = OtherSimpleTestElement(id, y).also { it.probability = this.probability }

    override fun contentEquivalent(other: Any?) = y == (other as? OtherSimpleTestElement)?.y
}
