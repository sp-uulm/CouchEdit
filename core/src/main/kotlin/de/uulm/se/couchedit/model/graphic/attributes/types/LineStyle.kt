package de.uulm.se.couchedit.model.graphic.attributes.types

import de.uulm.se.couchedit.model.attribute.types.EnumAttribute

class LineStyle() : EnumAttribute<LineStyle.Option>() {
    enum class Option {
        SOLID,
        DASHED
    }

    constructor(value: Option): this() {
        this.value = value
    }

    override fun getLegalValues(): List<Option> = Option.values().toList()

    override fun getDefault(): Option = Option.SOLID

    override fun getValueClass() = Option::class.java

    override fun copy(): LineStyle {
        val copy = LineStyle(this.value)

        return copy
    }

    companion object {
        val valueType = Option::class.java
    }
}
