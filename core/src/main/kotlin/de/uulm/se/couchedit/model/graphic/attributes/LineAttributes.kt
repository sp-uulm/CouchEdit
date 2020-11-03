package de.uulm.se.couchedit.model.graphic.attributes

import de.uulm.se.couchedit.model.attribute.Attribute
import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys.LINE_END_STYLE
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys.LINE_START_STYLE
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys.LINE_STYLE
import de.uulm.se.couchedit.model.graphic.attributes.types.LineEndPointStyle
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle

/**
 * AttributeBag defining the general appearance of a [de.uulm.se.couchedit.model.graphic.shapes.Line]'s shape.
 *
 * The style defined here is purely decorative and does not affect the spatial relations of the Element the line belongs
 * to.
 */
class LineAttributes(override val id: String) : AttributeBag() {
    fun getLineStyle(): LineStyle.Option? {
        return this[LINE_STYLE]?.value
    }

    fun getStartStyle(): LineEndPointStyle.Option? {
        return this[LINE_START_STYLE]?.value
    }

    fun getEndStyle(): LineEndPointStyle.Option? {
        return this[LINE_END_STYLE]?.value
    }

    override fun copy(): LineAttributes {
        val ret = LineAttributes(id)

        ret.setFrom(this)

        return ret
    }

    override fun getDefaults(): Map<AttributeReference<*>, Attribute<*>> = mapOf(
            LINE_STYLE to LineStyle(LineStyle.Option.SOLID),
            LINE_START_STYLE to LineEndPointStyle(LineEndPointStyle.Option.NONE),
            LINE_END_STYLE to LineEndPointStyle(LineEndPointStyle.Option.NONE)
    )


    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
}
