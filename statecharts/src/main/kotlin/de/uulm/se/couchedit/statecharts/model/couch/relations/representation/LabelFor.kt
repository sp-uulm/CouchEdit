package de.uulm.se.couchedit.statecharts.model.couch.relations.representation

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Label

abstract class LabelFor<B: Element>(
        a: ElementReference<ShapedElement<Label>>,
        b: ElementReference<B>
): OneToOneRelation<ShapedElement<Label>, B>(a, b) {
    override val isDirected = true
}
