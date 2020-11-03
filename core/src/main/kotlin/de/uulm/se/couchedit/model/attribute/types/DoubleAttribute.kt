package de.uulm.se.couchedit.model.attribute.types

import de.uulm.se.couchedit.model.attribute.Attribute

class DoubleAttribute() : Attribute<Double>() {
    constructor(value: Double): this() {
        this.value = value
    }

    override fun copy(): DoubleAttribute {
        return DoubleAttribute(value)
    }

    override fun getDefault(): Double = 0.0

    override fun getValueClass() = Double::class.java
}
