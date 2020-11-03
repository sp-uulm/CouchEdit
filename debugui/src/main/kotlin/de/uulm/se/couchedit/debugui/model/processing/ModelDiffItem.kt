package de.uulm.se.couchedit.debugui.model.processing

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Wrapper for a [ModelDiff] ([sourceModelDiff]) that can be displayed in a JavaFX Table View, along with its vector timestamp
 * (as represented by [timestampValues]) if the ModelDiff was in a TimedDiffCollection.
 */
class ModelDiffItem(sourceModelDiff: ModelDiff, timestamp: VectorTimestamp? = null) {
    /**
     * Type of the ModelDiff (Add, Remove, Modify)
     */
    val typeProperty = SimpleObjectProperty<DiffType>(diffTypeFromModelDiff(sourceModelDiff))

    /**
     * Element as stored in [ModelDiff.affected]
     */
    val affectedValueProperty = SimpleObjectProperty<Element>(sourceModelDiff.affected)

    /**
     * ID of the Element as stored in [ModelDiff.affected]
     */
    val affectedElementIdProperty = SimpleStringProperty(sourceModelDiff.affected.id)

    /**
     * "Simple" type name of the Element as stored in [ModelDiff.affected]
     */
    val affectedElementTypeProperty = SimpleStringProperty(sourceModelDiff.affected::class.simpleName)

    /**
     * Vector timestamp associated with this diff in the DiffCollection if it comes from a TimedDiffCollection
     */
    val timestampProperty = SimpleObjectProperty<VectorTimestamp>(timestamp)

    enum class DiffType {
        ADD,
        REMOVE,
        MODIFY
    }

    companion object {
        /**
         * Resolves a [DiffType] object from a [modelDiff].
         */
        private fun diffTypeFromModelDiff(modelDiff: ModelDiff): DiffType {
            return when (modelDiff) {
                is ElementAddDiff -> DiffType.ADD
                is ElementModifyDiff -> DiffType.MODIFY
                is ElementRemoveDiff -> DiffType.REMOVE
                else -> throw IllegalArgumentException("Unknown model diff type: ${modelDiff::class.qualifiedName}")
            }
        }
    }
}
