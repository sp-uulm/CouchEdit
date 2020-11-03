package de.uulm.se.couchedit.client.viewmodel.attribute

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/**
 * Data object to be shown in a TreeView of AttributeBag contents.
 *
 * This class is applicable to AttributeBags as well as to Attributes, thus the actual observable content is stored in
 * the dedicated [content] property. This property decides how the [AttributeViewModel] is to be handled.
 */
open class AttributeViewModel(name: String, simpleTypeName: String, content: AttributeContentViewModel? = null) {
    val nameProperty = SimpleStringProperty(name)
    var name: String by nameProperty

    val simpleTypeNameProperty = SimpleStringProperty(simpleTypeName)
    var simpleTypeName: String by simpleTypeNameProperty

    val contentProperty = SimpleObjectProperty(content)
    var content: AttributeContentViewModel? by contentProperty

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeViewModel

        if (name != other.name) return false
        if (simpleTypeName != other.simpleTypeName) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + simpleTypeName.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }


}
