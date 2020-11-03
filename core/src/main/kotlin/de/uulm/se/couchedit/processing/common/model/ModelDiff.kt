package de.uulm.se.couchedit.processing.common.model

import de.uulm.se.couchedit.model.Copyable
import de.uulm.se.couchedit.model.base.Element

abstract class ModelDiff: Copyable {
    /**
     * The [Element] that was subject of this diff in its changed state after the operation
     */
    abstract val affected: Element

    abstract override fun copy(): ModelDiff
}
