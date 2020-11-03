package de.uulm.se.couchedit.export.model.basic

import de.uulm.se.couchedit.export.model.SerializableObject

class SerEnum : SerializableObject {
    var enumClass: String? = null

    var valueName: String? = null

    var valueOrdinal: Int? = null
}
