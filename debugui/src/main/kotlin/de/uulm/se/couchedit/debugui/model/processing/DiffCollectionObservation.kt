package de.uulm.se.couchedit.debugui.model.processing

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Wrapper for a [DiffCollection] that can be displayed in a JavaFX table view.
 */
class DiffCollectionObservation(
        timestamp: Date,
        diffCollection: DiffCollection,
        containedElementTypes: Set<Class<out Element>>,
        totalCount: Int,
        addCount: Int,
        modifyCount: Int,
        removeCount: Int
) {
    /**
     * Property for the exact time on which the DebugLoggingModificationPort has received the [diffCollection].
     */
    val timestampProperty = SimpleObjectProperty(timestamp)

    /**
     * Property for the time the DiffCollection was recorded, [timestampProperty] formatted as given by [timeFormat]
     */
    val timeProperty = timestampProperty.objectBinding {
        timeFormat.format(it)
    }

    /**
     * The "raw" DiffCollection represented by this Observation
     */
    val diffCollectionProperty = SimpleObjectProperty<DiffCollection>(diffCollection)

    /**
     * Set of element types contained in this DiffCollection
     */
    val containedElementTypeProperty = SimpleObjectProperty<Set<Class<out Element>>>(containedElementTypes)

    /**
     * Set of element types contained in this DiffCollection, as formatted string of "simple" class names
     */
    val containedElementTypeSimpleNamesProperty = containedElementTypeProperty.objectBinding { set ->
        set?.joinToString(", ", "[", "]") { it.simpleName }
    }

    /**
     * Total number of ModelDiffs contained in the represented [DiffCollection]
     */
    val totalCountProperty = SimpleIntegerProperty(totalCount)

    /**
     * Total number of ElementAddDiffs contained in the represented [DiffCollection]
     */
    val addCountProperty = SimpleIntegerProperty(addCount)

    /**
     * Total number of ElementModifyDiffs contained in the represented [DiffCollection]
     */
    val modifyCountProperty = SimpleIntegerProperty(modifyCount)

    /**
     * Total number of ElementRemoveDiffs contained in the represented [DiffCollection]
     */
    val removeCountProperty = SimpleIntegerProperty(removeCount)

    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS")
    }
}
