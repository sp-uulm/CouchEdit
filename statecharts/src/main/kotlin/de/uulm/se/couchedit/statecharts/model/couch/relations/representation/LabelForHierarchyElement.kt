package de.uulm.se.couchedit.statecharts.model.couch.relations.representation

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateHierarchyElement

class LabelForHierarchyElement(
        a: ElementReference<ShapedElement<Label>>,
        b: ElementReference<StateHierarchyElement>,
        override var probability: ProbabilityInfo?
) : LabelFor<StateHierarchyElement>(a, b) {
    override fun copy(): LabelForHierarchyElement {
        return LabelForHierarchyElement(a, b, probability)
    }
}
