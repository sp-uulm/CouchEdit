package de.uulm.se.couchedit.model.base.suggestions

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

/**
 * OneToOneRelation assigning a [BaseSuggestion] to a [GraphicObject].
 */
class SuggestionFor<out A : BaseSuggestion, out B : GraphicObject<*>>(suggestion: ElementReference<A>, graphicObject: ElementReference<B>)
    : OneToOneRelation<A, B>(suggestion, graphicObject) {
    override val id = "Suggestion_${suggestion.id}_for_${graphicObject.id}"

    override var probability: ProbabilityInfo? = null

    override fun copy() = SuggestionFor(a, b)
}
