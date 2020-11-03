package de.uulm.se.couchedit.model.graphic.attributes

import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.graphic.attributes.types.LineEndPointStyle
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle

/**
 * Constants for AttributeBag contents pertaining to GraphicObjects.
 */
object GraphicAttributeKeys {
    val LINE_STYLE = AttributeReference("line_style", LineStyle::class.java)

    val LINE_START_STYLE = AttributeReference("line_start_style", LineEndPointStyle::class.java)
    val LINE_END_STYLE = AttributeReference("line_end_style", LineEndPointStyle::class.java)
}
