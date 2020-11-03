package de.uulm.se.couchedit.statecharts.model.couch.relations.representation

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement

class RepresentsStateElement(
        a: ElementReference<GraphicObject<*>>,
        b: ElementReference<StateElement>
) : Represents<GraphicObject<*>, StateElement>(a, b) {
    override fun copy() = RepresentsStateElement(a, b)
}
