package de.uulm.se.couchedit.client.interaction

import com.google.inject.Inject
import javafx.collections.FXCollections
import javafx.collections.ObservableMap

/**
 * Central registry hosting the set of [Tool]s that currently are available to the user.
 *
 * @param platformToolMap Map containing the [Tool]s defined by the frontend itself, i.e. that can be used to create
 *                        atomic PrimitiveGraphicObjects
 */
class ToolRegistry @Inject constructor(platformToolMap: Map<ToolDefinition, @JvmSuppressWildcards Tool>) {
    /**
     * Observable mapping of [ToolDefinition]s (controlling the user-facing description of the tools) to the Tool
     * instances associated with them.
     */
    val toolsObservableMap: ObservableMap<out Any, ToolGroup>

    init {
        val platformToolGroup = ToolGroup("Shapes", FXCollections.observableMap(platformToolMap.toMutableMap()))

        toolsObservableMap = FXCollections.observableMap(mutableMapOf(
                PlatformToolIdentifier to platformToolGroup
        ))
    }

    object PlatformToolIdentifier
}
