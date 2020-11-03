package de.uulm.se.couchedit.model.attribute.types

import de.uulm.se.couchedit.model.attribute.Attribute

class StringAttribute() : Attribute<String>() {
    constructor(value: String): this() {
        this.value = value
    }

    override fun copy(): StringAttribute {
        return StringAttribute(value)
    }

    override fun getDefault(): String = ""

    override fun getValueClass() = String::class.java
}
