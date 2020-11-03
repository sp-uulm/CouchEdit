package de.uulm.se.couchedit.model.connection.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line

/**
 * Represents an end of a connection which is  attached from a line (the [a] element) to another element [b].
 */
class ConnectionEnd<out A : GraphicObject<Line>, out B : ShapedElement<*>>(
        line: ElementReference<A>,
        attachedObject: ElementReference<B>,
        val isEndConnection: Boolean,
        override var probability: ProbabilityInfo?
) : OneToOneRelation<A, B>(line, attachedObject) {
    override val isDirected: Boolean get() = true

    override fun copy() = ConnectionEnd(a, b, isEndConnection, probability?.copy())

    override val id: String
        get() = "${this.javaClass.simpleName}_${a}_${b}"

    override fun toString(): String = "CE (line=$a, attached=$b, isEnd=$isEndConnection, prob=$probability)"
}
