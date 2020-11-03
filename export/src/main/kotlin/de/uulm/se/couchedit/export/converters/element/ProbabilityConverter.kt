package de.uulm.se.couchedit.export.converters.element

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.SerProbabilityInfo
import de.uulm.se.couchedit.export.model.SerProbabilityInfo.SerExplicit
import de.uulm.se.couchedit.export.model.SerProbabilityInfo.SerGenerated
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo.*

class ProbabilityConverter : AbstractConverter<ProbabilityInfo, SerProbabilityInfo>() {
    override fun toSerializable(element: ProbabilityInfo, context: ToSerializableContext): SerProbabilityInfo {
        return when (element) {
            is Generated -> {
                SerGenerated().apply { probability = element.probability }
            }
            Explicit -> SerExplicit()
        }
    }

    override fun fromSerializable(serializable: SerProbabilityInfo, context: FromSerializableContext): ProbabilityInfo {
        return when (serializable) {
            is SerGenerated -> {
                Generated(SerGenerated::probability.getNotNull(serializable))
            }
            is SerExplicit -> Explicit
        }
    }
}
