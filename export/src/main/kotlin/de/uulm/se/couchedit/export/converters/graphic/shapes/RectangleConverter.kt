package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.graphic.shapes.SerRectangle
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle

class RectangleConverter: RectangularConverter<Rectangle, SerRectangle>() {
    override fun toSerializable(element: Rectangle, context: ToSerializableContext): SerRectangle {
        return SerRectangle().setRectangularPropertiesFrom(element)
    }

    override fun fromSerializable(serializable: SerRectangle, context: FromSerializableContext): Rectangle {
        val (x, y, w, h) = serializable.getRectangularProperties()
        return Rectangle(x, y, w, h)
    }
}
