package de.uulm.se.couchedit.export.converters.element

import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.export.model.SerOneToOneRelation
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation

/**
 * [ElementConverter] extension that provides convenience functions for converting [OneToOneRelation]s
 * to [SerOneToOneRelation]s and vice versa.
 */
abstract class OneToOneRelationConverter<T : OneToOneRelation<*, *>, S : SerOneToOneRelation> : ElementConverter<T, S>() {
    /**
     * Sets the receiver [SerOneToOneRelation]'s [SerOneToOneRelation.a] and [SerOneToOneRelation.b] properties to the
     * equivalents of the parameter's [OneToOneRelation.a] and [OneToOneRelation.b] values.
     */
    protected fun <T : SerOneToOneRelation> T.setRelationEndPointsFrom(
            element: OneToOneRelation<*, *>,
            context: ToSerializableContext
    ): T {
        a = checkedConvertToSerializable(element.a, context)
        b = checkedConvertToSerializable(element.b, context)

        return this
    }

    /**
     * Returns a checked and casted Pair of the [OneToOneRelation.a] and [OneToOneRelation.b] [ElementReference]s
     * from the receiver [SerOneToOneRelation]'s [de.uulm.se.couchedit.export.model.SerElementReference]s.
     */
    protected inline fun <reified A : Element, reified B : Element> SerOneToOneRelation.getEndPoints(
            context: FromSerializableContext
    ): Endpoints<A, B> {
        val serA = SerOneToOneRelation::a.getNotNull(this)
        val serB = SerOneToOneRelation::b.getNotNull(this)

        val a = checkedConvertFromSerializable<ElementReference<*>>(serA, context)
        val b = checkedConvertFromSerializable<ElementReference<*>>(serB, context)

        return Endpoints(a.asType(), b.asType())

    }

    protected data class Endpoints<A : Element, B : Element>(val a: ElementReference<A>, val b: ElementReference<B>)
}
