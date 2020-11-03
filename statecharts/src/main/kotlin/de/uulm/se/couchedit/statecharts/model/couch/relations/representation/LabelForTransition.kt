package de.uulm.se.couchedit.statecharts.model.couch.relations.representation

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.RepresentsTransition

class LabelForTransition(
        a: ElementReference<ShapedElement<Label>>,
        b: ElementReference<RepresentsTransition>,
        override var probability: ProbabilityInfo? = null
) : LabelFor<RepresentsTransition>(a, b) {
    override fun copy() = LabelForTransition(a, b, probability?.copy())
}
