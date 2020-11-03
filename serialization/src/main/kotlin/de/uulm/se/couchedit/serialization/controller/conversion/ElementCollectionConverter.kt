package de.uulm.se.couchedit.serialization.controller.conversion

import com.google.inject.Inject
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AggregateConverter
import de.uulm.se.couchedit.export.exceptions.NoSuchConverterFromDataType
import de.uulm.se.couchedit.export.model.SerTimestamp
import de.uulm.se.couchedit.export.model.SerializableElement
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.PreparedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.serialization.model.ElementCollection
import de.uulm.se.couchedit.serialization.model.ElementInfo

class ElementCollectionConverter @Inject constructor(
        private val aggregateConverter: AggregateConverter,
        private val diffCollectionFactory: DiffCollectionFactory
) {
    fun convertDiffCollection(
            diffs: TimedDiffCollection,
            toSerializableContext: ToSerializableContext = ToSerializableContext()
    ): ElementCollection {
        val set = mutableSetOf<ElementInfo>()

        for (diff in diffs) {
            if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                val element = diff.affected

                val serElement = try {
                    aggregateConverter.convertToSerializable(element, toSerializableContext) as SerializableElement
                } catch (e: NoSuchConverterFromDataType) {
                    continue
                }

                val timestamp = diffs.getVersionForElement(element.id)
                val serTimestamp = aggregateConverter.convertToSerializable(timestamp, toSerializableContext) as? SerTimestamp

                set.add(ElementInfo(serElement, serTimestamp))
            }
        }

        return ElementCollection(set)
    }

    fun convertElementCollection(
            elementCollection: ElementCollection,
            fromSerializableContext: FromSerializableContext = FromSerializableContext()
    ): PreparedDiffCollection {
        val ret = diffCollectionFactory.createPreparedDiffCollection()

        for (elementInfo in elementCollection.elements) {
            val serElement = elementInfo.element ?: continue

            val element = aggregateConverter.convertFromSerializable(serElement, fromSerializableContext) as? Element
                    ?: continue

            ret.putDiff(ElementAddDiff(element))
        }

        return ret
    }
}
