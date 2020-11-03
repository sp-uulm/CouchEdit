package de.uulm.se.couchedit.debugui.view.processing

import de.uulm.se.couchedit.debugui.model.processing.ModelDiffItem
import de.uulm.se.couchedit.debugui.view.element.ElementDetailFragment
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Orientation
import tornadofx.*

/**
 * [Fragment] combining an [ElementDetailFragment] and an [VectorTimestampDetailFragment] in a vertical SplitPane
 * to show properties and Timestamp of a single [ModelDiffItem].
 */
class ModelDiffItemDetailFragment : Fragment("ModelDiffItem Details") {
    private val localScope = Scope()

    /**
     * Fragment showing the details of the element affected by [modelDiffProperty] value
     */
    private val elementDetails = find<ElementDetailFragment>(localScope)

    /**
     * Fragment showing the timestamp of the [modelDiffProperty] value
     */
    private val timestampDetails = find<VectorTimestampDetailFragment>(localScope)

    /**
     * Currently displayed [ModelDiffItem].
     */
    val modelDiffProperty = SimpleObjectProperty<ModelDiffItem>()

    override val root = splitpane(Orientation.VERTICAL) {
        this += elementDetails
        this += timestampDetails
    }

    init {
        modelDiffProperty.select { it.affectedValueProperty }.addListener { _, _, newValue ->
            this.elementDetails.element = newValue
        }
        timestampDetails.vectorTimestampProperty.bind(modelDiffProperty.select { it.timestampProperty })
    }
}
