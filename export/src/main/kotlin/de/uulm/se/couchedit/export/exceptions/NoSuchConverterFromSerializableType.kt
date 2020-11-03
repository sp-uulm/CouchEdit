package de.uulm.se.couchedit.export.exceptions

import de.uulm.se.couchedit.export.model.SerializableObject

class NoSuchConverterFromSerializableType(val sourceClass: Class<out SerializableObject>): ImportExportException() {
    override fun toString(): String {
        return "No Converter found to convert $sourceClass to a Data Object."
    }
}
