package de.uulm.se.couchedit.export.converters.graphic

import com.google.inject.Singleton
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.element.ElementConverter
import de.uulm.se.couchedit.export.model.graphic.SerPrimitiveGraphicObject
import de.uulm.se.couchedit.export.model.graphic.shapes.SerializableShape
import de.uulm.se.couchedit.export.util.couch.getDataId
import de.uulm.se.couchedit.export.util.couch.getSerializableId
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Shape

@Singleton
class PrimitiveGraphicObjectConverter : ElementConverter<PrimitiveGraphicObject<*>, SerPrimitiveGraphicObject>() {
    override fun toSerializable(element: PrimitiveGraphicObject<*>, context: ToSerializableContext): SerPrimitiveGraphicObject {
        return SerPrimitiveGraphicObject().apply {
            id = element.id.getSerializableId(context)
            shape = checkedConvertToSerializable(element.shape, context)
            z = element.z
        }.setProbabilityFrom(element, context)
    }

    override fun fromSerializable(serializable: SerPrimitiveGraphicObject, context: FromSerializableContext): PrimitiveGraphicObject<*> {
        return PrimitiveGraphicObject(
                SerPrimitiveGraphicObject::id.getNotNull(serializable).getDataId(context),
                SerPrimitiveGraphicObject::shape.fromSerializableNotNull<SerializableShape, Shape>(serializable, context)
        ).setProbabilityFrom(serializable, context).apply {
            z = serializable.z ?: mutableListOf()
        }
    }
}
