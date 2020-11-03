package de.uulm.se.couchedit.client.viewmodel.attribute

import de.uulm.se.couchedit.model.attribute.Attribute
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import tornadofx.*

/**
 * [AttributeContentViewModel] data object to show the content of one "normal" [Attribute] item.
 *
 * The Attribute value must not be changed directly in order for change tracking to work (without introducing properties
 * into core objects like Attribute). Instead, user-driven changes may be executed directly via the [attributeValueProperty].
 * Changes from the controller (= the system back-end) are executed via the [backingAttributeProperty].
 *
 * When an update is made through the [attributeValueProperty], the ViewModel is marked "dirty" ([dirtyByUser]).
 * This means, it can be [commit]ed to apply the changes to the [backingAttribute], or [rollback]ed to rollback the changes made to the
 * ViewModel.
 *
 * If the ViewModel is marked [dirtyByUser] and a change from the controller's side is detected, the ViewModel is
 * additionally marked [dirtyByBacking]. This means there was a concurrent edit while the user is still editing (which
 * may potentially need resolving).
 */
class AtomicAttributeContentViewModel<T>(attribute: Attribute<T>) : AttributeContentViewModel() {
    private val dirtyByUserInternal = ReadOnlyBooleanWrapper(false)

    private val dirtyByBackingInternal = ReadOnlyBooleanWrapper(false)

    val dirtyByUser: ReadOnlyBooleanProperty
        get() = dirtyByUserInternal.readOnlyProperty;

    override val isDirty = dirtyByUser

    val dirtyByBacking: ReadOnlyBooleanProperty
        get() = dirtyByBackingInternal.readOnlyProperty;

    val backingAttributeProperty = SimpleObjectProperty(attribute).apply {
        addListener { _, oldValue, newValue ->
            synchronized(this@AtomicAttributeContentViewModel) {
                if (oldValue != newValue) {
                    if (newValue == attributeValueProperty.value) {
                        dirtyByUserInternal.value = false
                    }

                    dirtyByBackingInternal.value = dirtyByUserInternal.value

                    // if the user has not changed the Attribute manually, also update the displayed value
                    if (!dirtyByUserInternal.value) {
                        attributeValueProperty.value = newValue.value
                    }
                }
            }
        }
    }
    var backingAttribute: Attribute<T> by backingAttributeProperty

    /**
     * Current attribute value that has been selected by the user.
     */
    val attributeValueProperty = SimpleObjectProperty(attribute.value).apply {
        addListener { _, oldValue, newValue ->
            synchronized(this@AtomicAttributeContentViewModel) {
                if (oldValue != newValue) {
                    dirtyByUserInternal.value = newValue != this@AtomicAttributeContentViewModel.backingAttribute.value
                }
            }
        }
    }

    val attributeType: Class<out Attribute<T>> = attribute::class.java

    val attributeValueType: Class<*>? = (attribute.value as? Any)?.let { it::class.java }

    /**
     * Function to set the backing attribute without generics, checked by Reflection.
     */
    fun setBackingAttributeUnsafe(attribute: Attribute<*>) {
        if (!attributeType.isAssignableFrom(attribute::class.java)) {
            throw IllegalArgumentException(
                    "Cannot set AtomicAttributeViewModel from attribute type ${attributeType.name} to ${attribute::class.java.name}"
            )
        }

        if (attributeValueType?.isAssignableFrom(attribute.value!!::class.java) != true) {
            throw IllegalArgumentException(
                    "Cannot set AtomicAttributeViewModel from attribute value type ${attributeValueType?.name} to ${attribute.value!!::class.java.name}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        this.backingAttribute = attribute as Attribute<T>
    }

    fun commit() {
        synchronized(this) {
            backingAttributeProperty.value.value = attributeValueProperty.value
            clean()
        }
    }

    fun rollback() {
        synchronized(this) {
            attributeValueProperty.value = backingAttributeProperty.value.value
            clean()
        }
    }

    private fun clean() {
        this.dirtyByUserInternal.value = false
        this.dirtyByBackingInternal.value = false
    }
}
