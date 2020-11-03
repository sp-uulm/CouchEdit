package de.uulm.se.couchedit.export.exceptions

class NoSuchConverterFromDataType(val sourceClass: Class<*>): ImportExportException() {
    override fun toString(): String {
        return "No Converter available to convert $sourceClass into a serializable instance."
    }
}
