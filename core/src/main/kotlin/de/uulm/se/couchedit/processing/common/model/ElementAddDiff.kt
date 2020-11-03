package de.uulm.se.couchedit.processing.common.model

import de.uulm.se.couchedit.model.base.Element

/**
 * TODO maybe merge this with [ElementModifyDiff]. Although it may be useful to detect inconsistencies?
 */
class ElementAddDiff(val added: Element) : ModelDiff() {
    override val affected = added

    override fun copy() = ElementAddDiff(added.copy())
}
