package de.uulm.se.couchedit.export.model.attribute

import de.uulm.se.couchedit.export.model.SerializableObject

class SerAttribute : SerializableObject {
    var attributeClass: String? = null

    var valueClass: String? = null

    var value: Any? = null
}
