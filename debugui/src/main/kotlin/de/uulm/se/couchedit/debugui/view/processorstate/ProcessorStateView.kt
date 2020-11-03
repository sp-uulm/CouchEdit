package de.uulm.se.couchedit.debugui.view.processorstate

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.uulm.se.couchedit.debugui.controller.processorstate.ProcessorStateController
import de.uulm.se.couchedit.debugui.util.fx.generateView
import de.uulm.se.couchedit.debugui.view.element.ElementDetailFragment
import de.uulm.se.couchedit.model.base.ElementReference
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.SetChangeListener
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * View to get an overview of the current state of Processors available in the system.
 *
 * Contains these views:
 * * On top of the window, a [combobox] which contains all Processors detected within the ModificationBusManager of the
 *   system
 * * Below that, contains a vertical resizable [splitpane] of:
 *      * An [ElementGraphFragment] showing the Elements and Relations of the currently selected Processor's
 *        ModelRepository
 *      * An [ElementDetailFragment] showing the properties of the currently selected [Element] of the
 *        [ElementGraphFragment]
 */
class ProcessorStateView : Fragment("Processor states") {
    /**
     * As multiple ProcessorStateViews may be shown at one time, each one has its own components for them to operate
     * independently
     */
    private val localScope = Scope()

    private val controller: ProcessorStateController by inject(localScope)

    private val elementGraphFragment = find<ElementGraphFragment>(scope = localScope)

    private val elementDetailFragment = find<ElementDetailFragment>(scope = localScope)

    private val selectedElementRef = SimpleObjectProperty<ElementReference<*>?>()

    private val refreshDetailButton = button("Refresh") {
        graphic = generateView(FontAwesomeIcon.REFRESH)
        action { loadSelectedElementToDetailFragment(selectedElementRef.value) }
        enableWhen(selectedElementRef.isNotNull)
    }

    override val root = borderpane {
        top {
            hbox {
                alignment = Pos.CENTER_LEFT

                val processorLabel = label("Available Processors: ")
                val processorCombo = combobox(controller.currentProcessor, controller.availableProcessors) {
                    vgrow = Priority.ALWAYS
                    promptText = "< Please select one >"
                }

                processorLabel.labelFor = processorCombo
            }
        }

        center = splitpane(Orientation.HORIZONTAL) {
            this += elementGraphFragment.root
            this += borderpane {
                top = toolbar {
                    this += refreshDetailButton
                }
                center = elementDetailFragment.root
            }
        }
    }

    init {
        elementGraphFragment.modelRepository.bind(this.controller.currentModelRepository.objectBinding { it })

        this.selectedElementRef.addListener { _, _, newValue ->
            loadSelectedElementToDetailFragment(newValue)
        }

        elementGraphFragment.selectedElements.addListener { change: SetChangeListener.Change<out ElementReference<*>> ->
            this.selectedElementRef.value = change.set.singleOrNull()
        }
    }

    private fun loadSelectedElementToDetailFragment(selectedRef: ElementReference<*>?) {
        this.elementDetailFragment.element = selectedRef?.let(controller::getElementState)
    }
}
