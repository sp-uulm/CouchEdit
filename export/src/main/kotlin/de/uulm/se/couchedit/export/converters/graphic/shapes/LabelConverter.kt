package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.graphic.shapes.SerLabel
import de.uulm.se.couchedit.model.graphic.shapes.Label

class LabelConverter : RectangularConverter<Label, SerLabel>() {
    override fun toSerializable(element: Label, context: ToSerializableContext): SerLabel {
        return SerLabel().setRectangularPropertiesFrom(element).apply {
            text = element.text
        }
    }

    override fun fromSerializable(serializable: SerLabel, context: FromSerializableContext): Label {
        val (x, y, w, h) = serializable.getRectangularProperties()

        return Label(x, y, w, h,
                text = SerLabel::text.getNotNull(serializable)
        )
    }
}
