package de.uulm.se.couchedit.export.converters.connection

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.element.OneToOneRelationConverter
import de.uulm.se.couchedit.export.model.connection.SerConnectionEnd
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line

class ConnectionEndConverter : OneToOneRelationConverter<ConnectionEnd<*, *>, SerConnectionEnd>() {
    override fun toSerializable(element: ConnectionEnd<*, *>, context: ToSerializableContext): SerConnectionEnd {
        return SerConnectionEnd().setRelationEndPointsFrom(element, context).apply {
            isEndConnection = element.isEndConnection
        }.setProbabilityFrom(element, context)
    }

    override fun fromSerializable(serializable: SerConnectionEnd, context: FromSerializableContext): ConnectionEnd<*, *> {
        val (aRef, bRef) = serializable.getEndPoints<GraphicObject<Line>, GraphicObject<*>>(context)

        val isEndConnection = SerConnectionEnd::isEndConnection.getNotNull(serializable)

        return ConnectionEnd(aRef, bRef, isEndConnection, null).setProbabilityFrom(serializable, context)
    }

}
