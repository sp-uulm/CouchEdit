package de.uulm.se.couchedit.processing.common.model

import de.uulm.se.couchedit.model.base.Element

class ElementRemoveDiff(val removed: Element) : ModelDiff() {
    override val affected = removed

    override fun copy() = ElementRemoveDiff(removed.copy())
}
