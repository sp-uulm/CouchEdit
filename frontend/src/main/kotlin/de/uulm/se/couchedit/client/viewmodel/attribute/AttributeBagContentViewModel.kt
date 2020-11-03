package de.uulm.se.couchedit.client.viewmodel.attribute

import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import griffon.javafx.collections.ElementObservableList
import griffon.javafx.collections.GriffonFXCollections
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import tornadofx.*

/**
 * [AttributeContentViewModel] which represents an [AttributeBag] type, i.e. it does have sub-[AttributeViewModel]s
 * assigned to it. Because such a ViewModel always represents
 */
class AttributeBagContentViewModel(
        val attributeBagId: String,
        attributeBagType: Class<out AttributeBag>,
        initialFields: Map<AttributeReference<*>, AttributeViewModel>
) : AttributeContentViewModel() {
    val attributeObservable = FXCollections.observableMap<AttributeReference<*>, AttributeViewModel>(initialFields.toMutableMap())!!

    val attributeBagTypeProperty = SimpleObjectProperty(attributeBagType)

    var attributeBagType: Class<out AttributeBag> by attributeBagTypeProperty

    private val dirtyElementObservableList = ElementObservableList<ObservableValue<Boolean>>()

    override val isDirty = GriffonFXCollections.observableStream(dirtyElementObservableList).anyMatch(ObservableValue<Boolean>::getValue)

    init {
        attributeObservable.addListener { change: MapChangeListener.Change<out AttributeReference<*>, out AttributeViewModel> ->
            if (change.wasAdded()) {
                change.valueAdded.content?.let { dirtyElementObservableList.add(it.isDirty) }
            } else if (change.wasRemoved()) {
                change.valueRemoved.content?.let { dirtyElementObservableList.remove(it.isDirty) }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeBagContentViewModel

        if (attributeBagId != other.attributeBagId) return false

        return true
    }

    override fun hashCode(): Int {
        return attributeBagId.hashCode()
    }
}
