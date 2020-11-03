package de.uulm.se.couchedit.model.connection.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

class DirectedConnection<A : GraphicObject<*>, B : GraphicObject<*>>(a: ElementReference<A>, b: ElementReference<B>)
    : OneToOneRelation<A, B>(a, b) {
    override val id: String get() = this.javaClass.name + "_" + this.a + "_" + this.b

    override var probability: ProbabilityInfo? = null

    override val isDirected: Boolean = true

    override fun copy() = DirectedConnection(a, b).also {
        it.probability = this.probability
    }
}
