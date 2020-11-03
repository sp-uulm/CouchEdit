package de.uulm.se.couchedit.model.hotspot

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle

class TestHotSpotDefinition(val a: ElementReference<GraphicObject<Rectangle>>)
    : HotSpotDefinition<Rectangle>(a, null, setOf(a), Rectangle::class.java) {
    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    override fun copy() = TestHotSpotDefinition(a)

    override fun contentEquivalent(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestHotSpotDefinition

        if (a != other.a) return false

        return true
    }
}
