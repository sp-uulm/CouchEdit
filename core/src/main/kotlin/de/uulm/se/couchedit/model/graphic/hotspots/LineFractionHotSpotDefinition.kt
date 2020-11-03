package de.uulm.se.couchedit.model.graphic.hotspots

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition

/**
 * A [HotSpotDefinition] that references a point on the [a] line at [offset] * length.
 * Offsets <= 0 will always return the line's start point, Offsets >= 1 the endpoint
 */
class LineFractionHotSpotDefinition(
        a: ElementReference<GraphicObject<Line>>,
        val offset: Double = 0.5
): HotSpotDefinition<Point>(a, null, setOf(a), Point::class.java) {
    override val id = "${this::class.java.simpleName}_${a.id}_$offset"

    val a = this.aSet.first().asType<GraphicObject<Line>>()

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    override fun copy() = LineFractionHotSpotDefinition(a, offset)

    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && (other as? LineFractionHotSpotDefinition)?.offset == this.offset
    }
}
