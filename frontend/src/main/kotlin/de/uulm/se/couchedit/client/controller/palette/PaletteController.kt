package de.uulm.se.couchedit.client.controller.palette

import com.google.common.collect.HashBiMap
import de.uulm.se.couchedit.client.interaction.Tool
import de.uulm.se.couchedit.client.interaction.ToolDefinition
import de.uulm.se.couchedit.client.interaction.ToolGroup
import de.uulm.se.couchedit.client.interaction.ToolRegistry
import de.uulm.se.couchedit.client.util.gef.contentViewer
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import org.eclipse.gef.mvc.fx.domain.IDomain
import tornadofx.*

/**
 * Controller which synchronizes the selection state of the Palette view in the Tornado application with the GEF
 * [ToolModel] state.
 */
class PaletteController : Controller() {
    /**
     * Current editor context
     */
    private val domain: IDomain by di()
    private val toolRegistry: ToolRegistry by di()

    private val toolModel by lazy {
        domain.contentViewer.getAdapter(ToolModel::class.java)
    }

    private val selectedToolProperty = SimpleObjectProperty<Tool?>(null)
    private var selectedTool: Tool? by selectedToolProperty

    val selectedToolDefinitionProperty = SimpleObjectProperty<ToolDefinition?>(NONE_TOOL_DEF)
    var selectedToolDefinition: ToolDefinition? by selectedToolDefinitionProperty

    val toolGroups = FXCollections.observableList<ToolGroup>(mutableListOf())

    private val defToTool = HashBiMap.create<ToolDefinition, Tool?>()

    init {
        selectedToolProperty.bindBidirectional(toolModel.toolProperty)

        toolRegistry.toolsObservableMap.addListener { change: MapChangeListener.Change<out Any, out ToolGroup> ->
            regenerateToolGroups(change.map.values)
        }

        selectedToolDefinitionProperty.addListener { _, _, newDef ->
            if (newDef == null) {
                selectedTool = null

                return@addListener
            }

            selectedTool = defToTool[newDef]
        }
        selectedToolProperty.addListener { _, _, newTool ->
            synchronized(defToTool) {
                this.selectedToolDefinition = defToTool.inverse()[newTool] // Null if an unknown tool is selected.
            }
        }

        // TODO also listen for changes in toolgroups themselves
        regenerateToolGroups(toolRegistry.toolsObservableMap.values)
    }

    private fun regenerateToolGroups(newToolGroups: Collection<ToolGroup>) {
        val selectToolGroup = ToolGroup("", FXCollections.observableMap(mutableMapOf(NONE_TOOL_DEF to null)))

        val toolGroups = listOf(selectToolGroup) + newToolGroups

        synchronized(defToTool) {
            defToTool.clear()
            for (toolGroup in newToolGroups) {
                for ((def, tool) in toolGroup.tools) {
                    defToTool[def] = tool
                }
            }
        }

        this.toolGroups.setAll(toolGroups)
    }

    companion object {
        private val NONE_TOOL_DEF = ToolDefinition("None")
    }
}
