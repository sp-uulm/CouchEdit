package de.uulm.se.couchedit.debugui.view

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.uulm.se.couchedit.debugui.view.processing.DiffCollectionLogView
import de.uulm.se.couchedit.debugui.view.processorstate.ProcessorStateView
import tornadofx.*

class DebugView: Fragment("CouchEdit Debug", FontAwesomeIconView(FontAwesomeIcon.BUG)) {
    override val root = tabpane {
        tab(ProcessorStateView::class)
        tab(DiffCollectionLogView::class)
    }
}
