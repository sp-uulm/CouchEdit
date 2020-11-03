package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.graphic.shapes.SerPoint
import de.uulm.se.couchedit.model.graphic.shapes.Point

class PointConverter: AbstractConverter<Point, SerPoint>() {
    override fun toSerializable(element: Point, context: ToSerializableContext): SerPoint {
        return SerPoint().apply {
            x = element.x
            y = element.y
        }
    }

    override fun fromSerializable(serializable: SerPoint, context: FromSerializableContext): Point {
        return Point(
                SerPoint::x.getNotNull(serializable),
                SerPoint::y.getNotNull(serializable)
        )
    }
}
