package de.uulm.se.couchedit.debugui.controller.processing

import de.uulm.se.couchedit.debugui.model.processing.DiffCollectionObservation
import de.uulm.se.couchedit.debugui.model.processing.ModelDiffItem
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import tornadofx.*

class DiffCollectionObservationDetailController : Controller() {
    /**
     * The [DiffCollectionObservation] to be interpreted
     */
    val observation = SimpleObjectProperty<DiffCollectionObservation>(null)

    /**
     * The full timestamp assigned to the [observation].
     */
    val timestamp = observation.select { it.timestampProperty }

    /**
     * String summary of the contents of the [observation].
     */
    val stats = observation.objectBinding { newValue ->
        return@objectBinding newValue?.let { "Add: ${it.addCountProperty.get()}, Modify: ${it.modifyCountProperty.get()}, Remove: ${it.removeCountProperty.get()}" }
    }

    /**
     * The contents of the [DiffCollectionObservation.diffCollectionProperty], converted to [ModelDiffItem]s.
     */
    val modelDiffItems = FXCollections.observableArrayList<ModelDiffItem>()

    private val modelDiffs = observation.select { it.diffCollectionProperty }

    init {
        modelDiffs.addListener { _, _, newValue ->
            modelDiffItems.setAll(convertModelDiffs(newValue))
        }
    }

    /**
     * Reads [ModelDiffItem]s from the given [diffCollection]
     */
    private fun convertModelDiffs(diffCollection: DiffCollection): List<ModelDiffItem> {
        return diffCollection.diffs.let {
            it.map { (elementId, modelDiff) ->
                ModelDiffItem(modelDiff, (diffCollection as? TimedDiffCollection)?.getVersionForElement(elementId))
            }
        }
    }
}
