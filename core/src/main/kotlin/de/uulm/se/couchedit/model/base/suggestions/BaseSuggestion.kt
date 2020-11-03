package de.uulm.se.couchedit.model.base.suggestions

import de.uulm.se.couchedit.model.base.AbstractElement
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection

open class BaseSuggestion(override val id: String, var title: String, val applyActions: DiffCollection) : AbstractElement() {
    override var probability: ProbabilityInfo? = null

    override fun copy() = BaseSuggestion(id, title, applyActions)

    override fun contentEquivalent(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseSuggestion

        if (title != other.title) return false
        if (applyActions != other.applyActions) return false

        return true
    }
}
