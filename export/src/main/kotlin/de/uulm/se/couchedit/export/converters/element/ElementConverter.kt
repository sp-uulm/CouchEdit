package de.uulm.se.couchedit.export.converters.element

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.SerProbabilityInfo
import de.uulm.se.couchedit.export.model.SerializableElement
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo

/**
 * [AbstractConverter] extension that provides convenience functions needed when converting [Element]s to
 * [SerializableElement]s.
 */
abstract class ElementConverter<T : Element, S : SerializableElement> : AbstractConverter<T, S>() {
    /**
     * Sets the receiver Data Element's probability from the parameter Serializable Element's probability.
     *
     * @param serializable The Serializable Element from which the probability should be set.
     * @return receiver for method chaining
     */
    protected fun <X : Element> X.setProbabilityFrom(serializable: SerializableElement, context: FromSerializableContext): X {
        val prob = serializable.probability?.let {
            checkedConvertFromSerializable<ProbabilityInfo>(it, context)
        }

        this.probability = prob

        return this
    }

    /**
     * Sets the receiver's probability from the parameter element's probability.
     *
     * @param element The data Element from which the probability should be read
     * @return Receiver for method chaining
     */
    protected fun <X : SerializableElement> X.setProbabilityFrom(element: Element, context: ToSerializableContext): X {
        val serProb = element.probability?.let {
            checkedConvertToSerializable<SerProbabilityInfo>(it, context)
        }

        this.probability = serProb

        return this
    }
}
