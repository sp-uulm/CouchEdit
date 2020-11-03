package de.uulm.se.couchedit.debugui.view.processing

import de.uulm.se.couchedit.debugui.controller.processing.VectorTimestampDetailController
import de.uulm.se.couchedit.debugui.model.processing.VectorTimestampItem
import javafx.scene.Parent
import tornadofx.*

/**
 * [Fragment] showing a table of the VersionManager IDs together with the time values of a certain VectorTimestamp
 */
class VectorTimestampDetailFragment : Fragment("Vector Timestamp Display") {
    private val controller: VectorTimestampDetailController by inject()

    /**
     * Currently evaluated VectorTimestamp.
     */
    val vectorTimestampProperty = controller.vectorTimestampProperty

    override val root: Parent = tableview(controller.timestampItems) {
        column("VersionManager ID", VectorTimestampItem::versionManagerIdProperty)
        column("Value", VectorTimestampItem::valueProperty)
    }
}
