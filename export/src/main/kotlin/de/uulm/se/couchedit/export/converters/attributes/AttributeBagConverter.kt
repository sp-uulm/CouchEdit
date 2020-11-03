package de.uulm.se.couchedit.export.converters.attributes

import com.google.inject.Inject
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.element.ElementConverter
import de.uulm.se.couchedit.export.exceptions.IllegalPropertyValue
import de.uulm.se.couchedit.export.exceptions.InstantiationImpossible
import de.uulm.se.couchedit.export.model.attribute.SerAttributeBag
import de.uulm.se.couchedit.export.model.attribute.SerAttributeBagItem
import de.uulm.se.couchedit.export.util.couch.getDataId
import de.uulm.se.couchedit.export.util.couch.getSerializableId
import de.uulm.se.couchedit.export.util.reflect.ClassToStringConverter
import de.uulm.se.couchedit.model.attribute.Attribute
import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag

class AttributeBagConverter @Inject constructor(
        private val classToStringConverter: ClassToStringConverter
) : ElementConverter<AttributeBag, SerAttributeBag>() {
    override fun toSerializable(element: AttributeBag, context: ToSerializableContext): SerAttributeBag {
        val serAttributeBag = SerAttributeBag().apply {
            id = element.id.getSerializableId(context)
            bagClass = classToStringConverter.classToString(element::class.java)
        }.setProbabilityFrom(element, context)

        val contentList = mutableListOf<SerAttributeBagItem>()

        for ((ref, attribute) in element.readOnlyValues) {
            val attributeBagItem = SerAttributeBagItem().apply {
                attributeId = ref.attrId
                attributeValue = checkedConvertToSerializable(attribute, context)
            }

            contentList.add(attributeBagItem)
        }

        serAttributeBag.bagValues = contentList

        return serAttributeBag
    }

    override fun fromSerializable(serializable: SerAttributeBag, context: FromSerializableContext): AttributeBag {
        val bagClassName = SerAttributeBag::bagClass.getNotNull(serializable)

        val bagClass = classToStringConverter.getClassFromString(bagClassName, AttributeBag::class.java)
                ?: throw IllegalPropertyValue(
                        serializable::class,
                        AttributeBag::class,
                        SerAttributeBag::bagClass,
                        bagClassName,
                        "Expected AttributeBag subtype!"
                )

        /*
         * AttributeBags are expected to have a one-string-argument constructor, the sole argument being
         * the AttributeBag's ID.
         */
        val constructor = try {
            bagClass.getDeclaredConstructor(String::class.java)
        } catch (e: NoSuchMethodException) {
            throw InstantiationImpossible(
                    serializable::class.java,
                    bagClass,
                    "No single-argument (id) constructor found"
            )
        }

        val id = SerAttributeBag::id.getNotNull(serializable).getDataId(context)

        val attributeBag = constructor.newInstance(id)

        val serializedBagValues = SerAttributeBag::bagValues.getNotNull(serializable)

        for (item in serializedBagValues) {
            val attributeId = SerAttributeBagItem::attributeId.getNotNull(item)
            val serAttribute = SerAttributeBagItem::attributeValue.getNotNull(item)

            val attribute = checkedConvertFromSerializable<Attribute<*>>(serAttribute, context)

            attributeBag[AttributeReference(attributeId, attribute::class.java)] = attribute
        }

        return attributeBag
    }
}
