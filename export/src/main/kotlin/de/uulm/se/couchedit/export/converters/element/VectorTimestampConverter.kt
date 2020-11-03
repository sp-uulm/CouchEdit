package de.uulm.se.couchedit.export.converters.element

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.SerTimestamp
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp

class VectorTimestampConverter : AbstractConverter<VectorTimestamp, SerTimestamp>() {
    override fun toSerializable(element: VectorTimestamp, context: ToSerializableContext): SerTimestamp {
        return SerTimestamp().apply {
            contents = element.toMap()
        }
    }

    override fun fromSerializable(serializable: SerTimestamp, context: FromSerializableContext): VectorTimestamp {
        val contents = SerTimestamp::contents.getNotNull(serializable)

        return VectorTimestamp(contents.toMutableMap())
    }
}
