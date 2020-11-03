package de.uulm.se.couchedit.export.converters.element

import com.google.inject.Inject
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.exceptions.IllegalPropertyValue
import de.uulm.se.couchedit.export.model.SerElementReference
import de.uulm.se.couchedit.export.util.couch.getDataId
import de.uulm.se.couchedit.export.util.couch.getSerializableId
import de.uulm.se.couchedit.export.util.reflect.ClassToStringConverter
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference

/**
 * [AbstractConverter] implementation that converts [ElementReference]s to [SerElementReference]s and vice versa.
 */
class ElementReferenceConverter @Inject constructor(
        private val classToStringConverter: ClassToStringConverter
) : AbstractConverter<ElementReference<*>, SerElementReference>() {
    override fun toSerializable(element: ElementReference<*>, context: ToSerializableContext): SerElementReference {
        return SerElementReference().apply {
            id = element.id.getSerializableId(context)
            // store type as a string as some serialization libraries (Genson) seem to have problems with Class instance
            type = classToStringConverter.classToString(element.type)
        }
    }

    override fun fromSerializable(serializable: SerElementReference, context: FromSerializableContext): ElementReference<*> {
        val type = SerElementReference::type.getNotNull(serializable)

        val clazz = classToStringConverter.getClassFromString(type, Element::class.java) ?: throw IllegalPropertyValue(
                serializable::class,
                ElementReference::class,
                ElementReference<*>::type,
                type,
                "Expected Element subtype!"
        )

        return ElementReference(SerElementReference::id.getNotNull(serializable).getDataId(context), clazz)
    }
}
