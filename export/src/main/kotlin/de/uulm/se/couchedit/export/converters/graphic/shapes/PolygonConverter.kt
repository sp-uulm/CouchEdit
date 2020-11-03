package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.graphic.shapes.SerPoint
import de.uulm.se.couchedit.export.model.graphic.shapes.SerPolygon
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.Polygon

class PolygonConverter : AbstractConverter<Polygon, SerPolygon>() {
    override fun toSerializable(element: Polygon, context: ToSerializableContext): SerPolygon {
        return SerPolygon().apply {
            outerBorder = element.outerBorder.map { checkedConvertToSerializable<SerPoint>(it, context) }
            holes = element.holes.map { holePoints ->
                holePoints.map {
                    checkedConvertToSerializable<SerPoint>(it, context)
                }
            }
        }
    }

    override fun fromSerializable(serializable: SerPolygon, context: FromSerializableContext): Polygon {
        val outerBorder = SerPolygon::outerBorder.fromSerializableListNotNull<SerPoint, Point>(serializable, context)

        val serHoles = SerPolygon::holes.getNotNull(serializable)

        val holes = serHoles.map { serHole -> serHole.map { checkedConvertFromSerializable<Point>(it, context) } }

        return Polygon(outerBorder, holes)
    }
}
