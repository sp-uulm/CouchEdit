package de.uulm.se.couchedit.debugui.view.processing

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.uulm.se.couchedit.debugui.controller.processing.DiffCollectionLogController
import de.uulm.se.couchedit.debugui.view.element.ElementDetailFragment
import javafx.geometry.Orientation
import javafx.scene.control.Tooltip
import tornadofx.*

/**
 * View displaying a [splitpane], consisting of (left-to-right):
 * * A [DiffCollectionLogView] displaying the DiffCollectionObservations coming in via the [DiffCollectionLogController]'s
 *   DebugLoggingModificationPort
 * * A [DiffCollectionObservationDetailFragment] to display the contents of a DiffCollection selected in the
 *   [DiffCollectionLogView]
 * * A [ElementDetailFragment] displaying the details of the affected element of a ModelDiff selected in the
 *   [DiffCollectionObservationDetailFragment].
 */
class DiffCollectionLogView : View("DiffCollection Log") {
    private val controller: DiffCollectionLogController by inject(FX.defaultScope)

    /**
     * All subcomponents are injected via this scope, so that multiple DiffCollectionLogViews could coexist
     */
    private val localScope = Scope()

    private val diffCollectionLog = find<DiffCollectionObservationListFragment>(localScope).apply {
        diffCollectionList = controller.observedDiffs
    }

    val diffCollectionObservationDetailFragment = find<DiffCollectionObservationDetailFragment>(localScope).apply {
        displayedObservation.bind(diffCollectionLog.selectedObservation)
    }

    private val modelDiffItemDetailFragment = find<ModelDiffItemDetailFragment>(localScope).apply {
        modelDiffProperty.bind(diffCollectionObservationDetailFragment.selectedModelDiffItem)
    }

    override val root = borderpane {
        top {
            toolbar {
                button("Connect") {
                    addClass("icon-only")
                    graphic = FontAwesomeIconView(FontAwesomeIcon.PLUG).apply {
                        style {
                            fill = c("#818181")
                        }
                        glyphSize = 18
                    }
                    tooltip = Tooltip("Connect to ModificationBusManager (start listening)")
                    action { controller.connect() }
                }
            }
        }

        center {
            splitpane(Orientation.HORIZONTAL) {
                this += diffCollectionLog
                this += diffCollectionObservationDetailFragment
                this += modelDiffItemDetailFragment
            }
        }
    }
}
