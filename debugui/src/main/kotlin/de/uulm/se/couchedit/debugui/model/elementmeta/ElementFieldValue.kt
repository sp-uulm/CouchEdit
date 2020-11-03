package de.uulm.se.couchedit.debugui.model.elementmeta

import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/**
 * Represents a value that a variable of an Element has.
 */
open class ElementFieldValue(name: String, simpleTypeName: String, qualifiedTypeName: String, displayValue: String) {
    val nameProperty = SimpleStringProperty(name)
    var name: String by nameProperty

    val displayValueProperty = SimpleStringProperty(displayValue)
    var displayValue: String by displayValueProperty

    val simpleTypeNameProperty = SimpleStringProperty(simpleTypeName)
    var simpleTypeName: String by simpleTypeNameProperty

    val qualifiedTypeNameProperty = SimpleStringProperty(qualifiedTypeName)
    var qualifiedTypeName: String by qualifiedTypeNameProperty
}
