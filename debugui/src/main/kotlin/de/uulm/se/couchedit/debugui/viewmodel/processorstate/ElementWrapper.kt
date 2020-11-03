package de.uulm.se.couchedit.debugui.viewmodel.processorstate

import de.uulm.se.couchedit.debugui.util.StringUtil
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

/**
 * Wrapper class for an [ElementReference] to be shown in a JGraphX view.
 *
 * As JGraphX always seems to take the toString() of the value method as the label, the label display style also has
 * to be selected here.
 */
data class ElementWrapper<T: Element>(val ref: ElementReference<T>) {
    var displayStyle: DisplayStyle = DisplayStyle.ID_TYPE_NAME_CAMELHUMPS

    override fun toString(): String {
        return when(displayStyle) {
            DisplayStyle.ID_ONLY -> {
                ref.id
            }
            DisplayStyle.TYPE_NAME_CAMELHUMPS -> {
                StringUtil.toCamelHumps(ref.type.simpleName)
            }
            DisplayStyle.TYPE_NAME_FULL -> {
                ref.type.simpleName
            }
            DisplayStyle.ID_TYPE_NAME_CAMELHUMPS -> {
                ref.id + " [" + StringUtil.toCamelHumps(ref.type.simpleName) + "]"
            }
            DisplayStyle.ID_TYPE_NAME_FULL -> {
                ref.id + " [" + ref.type.simpleName + "]"
            }
        }
    }
}
