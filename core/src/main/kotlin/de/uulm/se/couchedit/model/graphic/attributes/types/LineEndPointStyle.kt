package de.uulm.se.couchedit.model.graphic.attributes.types

import de.uulm.se.couchedit.model.attribute.types.EnumAttribute

/**
 * Attribute defining the appearance of a [de.uulm.se.couchedit.model.graphic.shapes.Line] shape's start / end.
 *
 * The style defined here is purely decorative and does not affect the spatial relations of the Element the line belongs
 * to.
 */
class LineEndPointStyle(): EnumAttribute<LineEndPointStyle.Option>() {
    constructor(value: Option): this() {
        this.value = value
    }

    override fun getLegalValues(): List<Option> = Option.values().toList()

    override fun copy(): LineEndPointStyle {
        val copy = LineEndPointStyle()

        copy.value = this.value

        return copy
    }

    override fun getDefault() = Option.NONE

    override fun getValueClass() = Option::class.java

    enum class Option {
        NONE,
        SOLIDARROW
    }
}
