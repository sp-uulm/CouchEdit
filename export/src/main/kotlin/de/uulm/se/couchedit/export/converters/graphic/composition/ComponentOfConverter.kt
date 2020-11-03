package de.uulm.se.couchedit.export.converters.graphic.composition

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.element.OneToOneRelationConverter
import de.uulm.se.couchedit.export.model.graphic.composition.SerComponentOf
import de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

class ComponentOfConverter : OneToOneRelationConverter<ComponentOf<*, *>, SerComponentOf>() {
    override fun toSerializable(element: ComponentOf<*, *>, context: ToSerializableContext): SerComponentOf {
        return SerComponentOf().setProbabilityFrom(element, context).setRelationEndPointsFrom(element, context)
    }

    override fun fromSerializable(serializable: SerComponentOf, context: FromSerializableContext): ComponentOf<*, *> {
        val (a, b) = serializable.getEndPoints<GraphicObject<*>, GraphicObject<*>>(context)

        return ComponentOf(a, b).setProbabilityFrom(serializable, context)
    }
}
