package de.uulm.se.couchedit.export.converters.attributes

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.element.OneToOneRelationConverter
import de.uulm.se.couchedit.export.model.attribute.SerAttributesFor
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element

class AttributesForConverter : OneToOneRelationConverter<AttributesFor, SerAttributesFor>() {
    override fun toSerializable(element: AttributesFor, context: ToSerializableContext): SerAttributesFor {
        return SerAttributesFor().setRelationEndPointsFrom(element, context).setProbabilityFrom(element, context)
    }

    override fun fromSerializable(serializable: SerAttributesFor, context: FromSerializableContext): AttributesFor {
        val (a, b) = serializable.getEndPoints<AttributeBag, Element>(context)

        return AttributesFor(a, b).setProbabilityFrom(serializable, context)
    }
}
