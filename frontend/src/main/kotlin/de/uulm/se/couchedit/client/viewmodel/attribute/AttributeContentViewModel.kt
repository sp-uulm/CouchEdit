package de.uulm.se.couchedit.client.viewmodel.attribute

import griffon.javafx.collections.ElementObservableList
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.value.ObservableValue

abstract class AttributeContentViewModel: ElementObservableList.ObservableValueContainer {
    abstract val isDirty: ObservableValue<Boolean>

    override fun observableValues(): Array<ObservableValue<*>> {
        return arrayOf(isDirty)
    }
}
