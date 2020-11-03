package de.uulm.se.couchedit.debugui.model.elementmeta

import javafx.collections.FXCollections

/**
 * Represents a variable value of an Element which contains a variable type that has more readable fields available
 *
 * @param subFields The fields that the value of the variable with name [name] contains.
 */
class ComplexElementFieldValue(
        name: String,
        simpleTypeName: String,
        qualfiedTypeName: String,
        displayValue: String,
        subFields: Map<String, ElementFieldValue>)
    : ElementFieldValue(name, simpleTypeName, qualfiedTypeName, displayValue) {
    val subFieldsObservable = FXCollections.observableMap<String, ElementFieldValue>(subFields.toMutableMap())!!
}
