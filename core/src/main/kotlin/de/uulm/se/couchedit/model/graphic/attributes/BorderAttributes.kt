package de.uulm.se.couchedit.model.graphic.attributes

import de.uulm.se.couchedit.model.attribute.Attribute
import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle

/**
 * AttributeBag defining the appearance of the border of an Element's shape with an interior and a border.
 *
 * The style defined here is purely decorative and does not affect the spatial relations of the Element the shape belongs
 * to.
 */
class BorderAttributes(override val id: String) : AttributeBag() {
    fun getBorderStyle(): LineStyle.Option? {
        return this[GraphicAttributeKeys.LINE_STYLE]?.value
    }

    override fun copy(): BorderAttributes {
        val ret = BorderAttributes(id)

        ret.setFrom(this)

        return ret
    }

    override fun getDefaults(): Map<AttributeReference<*>, Attribute<*>> = mapOf(
            GraphicAttributeKeys.LINE_STYLE to LineStyle(LineStyle.Option.SOLID)
    )

    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
}
