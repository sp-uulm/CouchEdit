package de.uulm.se.couchedit.client.di

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder
import de.uulm.se.couchedit.client.interaction.Tool
import de.uulm.se.couchedit.client.interaction.ToolDefinition
import de.uulm.se.couchedit.client.interaction.creationtool.LabelCreationTool
import de.uulm.se.couchedit.client.interaction.creationtool.LineCreationTool
import de.uulm.se.couchedit.client.interaction.creationtool.RectangleCreationTool
import de.uulm.se.couchedit.client.interaction.creationtool.RoundedRectangleCreationTool
import de.uulm.se.couchedit.export.di.ExportModule
import de.uulm.se.couchedit.serialization.di.PersistenceModule
import de.uulm.se.couchedit.statecharts.di.StatechartsModule

/**
 * Central module used by the front end.
 */
internal class FrontendModule : AbstractModule() {
    override fun configure() {
        configureTools()

        install(PersistenceModule(ExportModule()))

        install(EclipseGefModule())

        install(StatechartsModule())
    }

    fun configureTools() {
        val tlToolDef = object : TypeLiteral<ToolDefinition>() {}
        val tlTool = object : TypeLiteral<Tool>() {}

        val binder = MapBinder.newMapBinder(binder(), tlToolDef, tlTool)

        for ((def, toolClass) in getTools()) {
            binder.addBinding(def).to(toolClass)
        }
    }

    fun getTools(): Map<ToolDefinition, Class<out Tool>> {
        return mapOf(
                ToolDefinition("Line") to LineCreationTool::class.java,
                ToolDefinition("Rect") to RectangleCreationTool::class.java,
                ToolDefinition("RoundRect") to RoundedRectangleCreationTool::class.java,
                ToolDefinition("Label") to LabelCreationTool::class.java
        )
    }
}
