package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.graphic.shapes.SerPoint
import de.uulm.se.couchedit.export.model.graphic.shapes.SerStraightSegmentLine
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine

class StraightSegmentLineConverter : AbstractConverter<StraightSegmentLine, SerStraightSegmentLine>() {
    override fun toSerializable(element: StraightSegmentLine, context: ToSerializableContext): SerStraightSegmentLine {
        return SerStraightSegmentLine().apply {
            points = element.points.map { checkedConvertToSerializable<SerPoint>(it, context) }
        }
    }

    override fun fromSerializable(serializable: SerStraightSegmentLine, context: FromSerializableContext): StraightSegmentLine {
        val points = SerStraightSegmentLine::points.fromSerializableListNotNull<SerPoint, Point>(serializable, context)

        return StraightSegmentLine(points)
    }
}
