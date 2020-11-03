package de.uulm.se.couchedit.processing.common.model

import de.uulm.se.couchedit.model.base.Element

class ElementModifyDiff(val before: Element, val after: Element) : ModelDiff() {
    override val affected = after

    init {
        if (before.id != after.id) {
            throw IllegalArgumentException(
                    String.format(
                            "The ID of element before and after a modification must be the same, but got before: %s and after: %s",
                            before.id,
                            after.id
                    )
            )
        }

        if (before.javaClass != after.javaClass) {
            throw IllegalArgumentException(
                    String.format(
                            "The Class of element before and after a modification must be the same, but got before: %s and after: %s",
                            before.javaClass,
                            after.javaClass
                    )
            )
        }
    }

    override fun copy() = ElementModifyDiff(before.copy(), after.copy())
}
