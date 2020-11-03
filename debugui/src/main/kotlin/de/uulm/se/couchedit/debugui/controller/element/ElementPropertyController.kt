package de.uulm.se.couchedit.debugui.controller.element

import de.uulm.se.couchedit.debugui.model.elementmeta.ComplexElementFieldValue
import de.uulm.se.couchedit.debugui.model.elementmeta.ElementFieldValue
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import tornadofx.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Controller that can read the variables of an [Element] instance via Reflection and produce [ElementFieldValue]
 * data objects from them.
 */
class ElementPropertyController : Controller() {
    /**
     * Property containing the [Element] currently being displayed, of which [elementFieldValues] represents the
     * variables and their values.
     */
    var element: Element? = null
        set(newValue) {
            val oldValue = field
            field = newValue
            if (oldValue != newValue || oldValue?.equivalent(newValue) != true) {
                newValue?.let { this.loadFields(newValue, elementFieldValues) } ?: elementFieldValues.clear()

                notifyDataLoaded()
            }
        }

    val elementFieldValues = FXCollections.observableHashMap<String, ElementFieldValue>()!!

    /**
     * Function that gets called with the new [elementProperty] value and values in [elementFieldValues] whenever
     * the element has changed and the corresponding elementFieldValues have been generated.
     */
    var newDataReadyListener: Function2<Element?, Collection<ElementFieldValue>, Unit>? = null

    /**
     * Function which converts the Field / Variable values of a given [Element] into [ElementFieldValue]s and writes
     * the results to [into].
     *
     * If a variable contains another [Element], then the [loadFields] method is called recursively to generate a
     * [ComplexElementFieldValue].
     */
    private fun loadFields(element: Any, into: MutableMap<String, ElementFieldValue>) {
        // collect names that are still present in the properties
        val availableNames = mutableListOf<String>()

        for (property in element::class.memberProperties) {
            val name = property.name
            val value = (property as KProperty1<Any, *>).get(element)
            val simpleClassName = value?.let {
                it::class.simpleName
            } ?: "null"
            val qualifiedClassName = value?.let {
                it::class.qualifiedName
            } ?: "null"

            availableNames.add(name)

            val storedValue = into[name]?.also {
                it.displayValue = value.toString()
                it.simpleTypeName = simpleClassName
                it.qualifiedTypeName = qualifiedClassName
            } ?: run {
                return@run if (value is Element || value is Shape) {
                    ComplexElementFieldValue(name, simpleClassName, qualifiedClassName, value.toString(), emptyMap())
                } else {
                    ElementFieldValue(name, simpleClassName, qualifiedClassName, value.toString())
                }
            }.also { into[name] = it }


            if (storedValue is ComplexElementFieldValue) {
                value?.let { loadFields(it, storedValue.subFieldsObservable) }
                        ?: storedValue.subFieldsObservable.clear()
            }
        }

        into.filterKeys { it in availableNames }
    }

    /**
     * Notifies the attached [newDataReadyListener] when the data of the current [elementProperty] value is fully loaded.
     */
    private fun notifyDataLoaded() {
        newDataReadyListener?.let { it(element, elementFieldValues.values) }
    }
}
