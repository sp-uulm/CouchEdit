package de.uulm.se.couchedit.debugui.controller.processing

import de.uulm.se.couchedit.debugui.model.processing.VectorTimestampItem
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import tornadofx.*

/**
 * [Controller] that can unpack a vector timestamp given to it via the [vectorTimestampProperty] and convert it
 * to an observable list of [timestampItems] in order for it to be displayed in a JavaFX GUI
 */
class VectorTimestampDetailController : Controller() {
    /**
     * Timestamp currently evaluated.
     */
    val vectorTimestampProperty = SimpleObjectProperty<VectorTimestamp>()

    /**
     * Representation of the contents of the value from [vectorTimestampProperty] as a list of [VectorTimestampItem]s
     */
    val timestampItems = FXCollections.observableArrayList<VectorTimestampItem>()

    init {
        vectorTimestampProperty.addListener { _, _, newValue ->
            newValue?.let { timestampItems.setAll(generateTimestampItems(it)) } ?: timestampItems.clear()
        }
    }

    /**
     * Generates a Collection of [VectorTimestampItem]s from a given [vectorTimestamp]
     */
    private fun generateTimestampItems(vectorTimestamp: VectorTimestamp): List<VectorTimestampItem> {
        return vectorTimestamp.entries.map { (versionManagerId, value) ->
            VectorTimestampItem(versionManagerId, value)
        }
    }
}
