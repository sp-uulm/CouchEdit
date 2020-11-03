package de.uulm.se.couchedit.debugui.view.processing

import de.uulm.se.couchedit.debugui.controller.processing.DiffCollectionObservationDetailController
import de.uulm.se.couchedit.debugui.model.processing.ModelDiffItem
import de.uulm.se.couchedit.debugui.view.DebugLayoutParams
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.Parent
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * A [Fragment] displaying details about a given [displayedObservation].
 *
 * Contains following elements:
 * * [label] showing the timestamp of the Observation
 * * [label] showing a summary of the element counts of the Observation
 * * [tableview] showing the [ModelDiffItem]s contained in the [displayedObservation].
 */
class DiffCollectionObservationDetailFragment : Fragment("DiffCollection Detail") {
    private val controller: DiffCollectionObservationDetailController by inject()

    private val timestampLabel = label(controller.timestamp).apply {
        padding = DebugLayoutParams.labelPadding
    }

    private val statsLabel = label(controller.stats).apply {
        padding = DebugLayoutParams.labelPadding
    }

    private val modelDiffTable = tableview<ModelDiffItem>(controller.modelDiffItems) {
        column("Type", ModelDiffItem::typeProperty)
        column("Affected ID", ModelDiffItem::affectedElementIdProperty)
        column("Type of Element", ModelDiffItem::affectedElementTypeProperty)
    }

    /**
     * The DiffCollectionObservation to be displayed in this [Fragment]
     */
    val displayedObservation = controller.observation

    /**
     * Property containing the [ModelDiffItem] currently selected in the [tableview] of this
     * Fragment
     */
    val selectedModelDiffItem: ReadOnlyObjectProperty<ModelDiffItem> = ReadOnlyObjectWrapper<ModelDiffItem>().apply(modelDiffTable::bindSelected)

    override val root: Parent = vbox {
        this += timestampLabel
        this += statsLabel
        this += modelDiffTable

        modelDiffTable.hgrow = Priority.ALWAYS
        modelDiffTable.vgrow = Priority.ALWAYS
    }
}
