package de.uulm.se.couchedit.client.view.palette

import com.google.common.collect.HashBiMap
import de.uulm.se.couchedit.client.controller.palette.PaletteController
import de.uulm.se.couchedit.client.interaction.ToolDefinition
import de.uulm.se.couchedit.client.interaction.ToolGroup
import javafx.collections.ListChangeListener
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * View that provides the user with tools / elements to use and manipulate the drawing.
 * @todo Currently hard-coded for the proof-of-concept, make this backend driven in the future
 */
class PaletteView : View() {
    override val root = vbox {}

    private val controller: PaletteController by inject()

    private val toolToggleButtonMap = HashBiMap.create<ToolDefinition?, ToggleButton>()

    /**
     * Group containing all the [ToggleButton]s used for switching between tools
     */
    private val toolToggleGroup = ToggleGroup()

    init {
        this.setUpToolSubscriber()
        this.setUpToolToggles(controller.toolGroups)

        this.controller.selectedToolDefinitionProperty.addListener { _, _, newValue ->
            selectToggleForDef(newValue)
        }

        selectToggleForDef(controller.selectedToolDefinition)
    }

    private fun setUpToolSubscriber() {
        this.controller.toolGroups.addListener { change: ListChangeListener.Change<out ToolGroup> ->
            setUpToolToggles(change.list)
        }
    }

    private fun setUpToolToggles(groups: Collection<ToolGroup>) {
        this.root.clear()
        toolToggleButtonMap.clear()

        var isFirst = true

        for (group in groups) {
            if (!isFirst) {
                this.root += separator { }
            }

            for (def in group.tools.keys) {
                val tb = ToggleButton(def.name)

                tb.selectedProperty().addListener { _, _, newVal ->
                    this.controller.selectedToolDefinition = if (newVal) def else null
                }

                tb.hgrow = Priority.ALWAYS
                tb.maxWidth = Double.MAX_VALUE

                tb.toggleGroup = this.toolToggleGroup
                this.root += tb

                toolToggleButtonMap[def] = tb
            }

            isFirst = false
        }
    }

    private fun selectToggleForDef(def: ToolDefinition?) {
        if(def == null) {

        }

        this.toolToggleGroup.selectToggle(toolToggleButtonMap[def])
    }
}
