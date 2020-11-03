package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.graphic.shapes.SerRoundedRectangle
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle

class RoundedRectangleConverter: RectangularConverter<RoundedRectangle, SerRoundedRectangle>() {
    override fun toSerializable(element: RoundedRectangle, context: ToSerializableContext): SerRoundedRectangle {
        return SerRoundedRectangle().setRectangularPropertiesFrom(element).apply {
            cornerRadius = element.cornerRadius
        }
    }

    override fun fromSerializable(serializable: SerRoundedRectangle, context: FromSerializableContext): RoundedRectangle {
        val (x, y, w, h) = serializable.getRectangularProperties()

        return RoundedRectangle(x, y, w, h,
                cornerRadius = SerRoundedRectangle::cornerRadius.getNotNull(serializable)
        )
    }
}
