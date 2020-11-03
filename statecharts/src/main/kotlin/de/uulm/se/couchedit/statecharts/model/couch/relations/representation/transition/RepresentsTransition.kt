package de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents

/**
 * Specifies that the given [a] [GraphicObject] represents the [b] [Transition].
 */
class RepresentsTransition(
        a: ElementReference<GraphicObject<*>>,
        b: ElementReference<Transition>
) : Represents<GraphicObject<*>, Transition>(a, b) {
    override fun copy() = RepresentsTransition(a, b)
}
