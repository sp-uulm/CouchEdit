package de.uulm.se.couchedit.client.view.attribute

import de.uulm.se.couchedit.client.view.attribute.edit.ComboBoxEditControl
import de.uulm.se.couchedit.client.view.attribute.edit.TableCellEditControl
import de.uulm.se.couchedit.client.viewmodel.attribute.AtomicAttributeContentViewModel
import de.uulm.se.couchedit.model.attribute.types.EnumAttribute

internal object TableCellEditControlFactory {
    /**
     * Returns the control appropriate for editing the given [attributeContentViewModel], or null if no suitable
     * control is available.
     */
    fun <T> getControl(attributeContentViewModel: AtomicAttributeContentViewModel<T>): TableCellEditControl<T>? {
        return when (val attr = attributeContentViewModel.backingAttribute) {
            is EnumAttribute<*> -> {
                ComboBoxEditControl(attributeContentViewModel)
            }
            else -> {
                null
            }
        }
    }
}

