package de.uulm.se.couchedit.debugui.view.processing

import de.uulm.se.couchedit.debugui.model.processing.DiffCollectionObservation
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.layout.*
import javafx.scene.paint.Color
import tornadofx.*

/**
 * [Fragment] displaying a list of [DiffCollectionObservation]s in a Table view.
 */
class DiffCollectionObservationListFragment : Fragment("DiffCollection List") {
    override val root = tableview<DiffCollectionObservation> {
        column("Time", DiffCollectionObservation::timeProperty)
        column("Contained Element Types", DiffCollectionObservation::containedElementTypeSimpleNamesProperty)
        column("Total", DiffCollectionObservation::totalCountProperty)
        column("Add", DiffCollectionObservation::addCountProperty)
        column("Modify", DiffCollectionObservation::modifyCountProperty)
        column("Remove", DiffCollectionObservation::removeCountProperty)

        // on double clicking a DiffCollectionObservation, the corresponding details should be displayed.
        val expander = rowExpander(expandOnDoubleClick = true) {
            background = Background(BackgroundFill(Color.WHITESMOKE, null, null))
            border = Border(BorderStroke(
                    Color.BLACK,
                    BorderStrokeStyle.DASHED,
                    CornerRadii(3.0),
                    BorderWidths(1.0),
                    Insets(3.0)
            ))

            // create a new scope so we don't steal any Fragment
            val rowScope = Scope()

            val detailFragment = find<DiffCollectionObservationDetailFragment>(rowScope)
            detailFragment.displayedObservation.value = it

            this += detailFragment
        }

        expander.isVisible = false
    }

    /**
     * The list of [DiffCollectionObservation]s to be displayed in the [root] table of this Fragment
     */
    val diffCollectionListProperty: ObjectProperty<ObservableList<DiffCollectionObservation>?> = SimpleObjectProperty(null)
    var diffCollectionList: ObservableList<DiffCollectionObservation>? by diffCollectionListProperty

    /**
     * The [DiffCollectionObservation] that is currently selected in the table view of this Fragment.
     */
    val selectedObservation: ReadOnlyObjectProperty<DiffCollectionObservation> = ReadOnlyObjectWrapper<DiffCollectionObservation>().apply(root::bindSelected)

    init {
        root.items = diffCollectionList

        diffCollectionListProperty.addListener { _, _, newValue ->
            root.items = newValue
        }
    }
}
