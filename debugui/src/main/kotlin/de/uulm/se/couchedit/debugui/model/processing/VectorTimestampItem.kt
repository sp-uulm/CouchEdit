package de.uulm.se.couchedit.debugui.model.processing

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Representation of a VectorTimestamp that can be shown in a JavaFX Table.
 */
class VectorTimestampItem(
        versionManagerId: String,
        value: Long
) {
    /**
     * ID of the VersionManager this time value belongs to
     */
    val versionManagerIdProperty = SimpleStringProperty(versionManagerId)

    val valueProperty = SimpleLongProperty(value)
}
