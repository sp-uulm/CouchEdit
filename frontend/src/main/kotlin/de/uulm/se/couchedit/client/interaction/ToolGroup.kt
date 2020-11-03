package de.uulm.se.couchedit.client.interaction

import javafx.collections.ObservableMap

/**
 * Represents a group of tools that will be shown to the user in the user interface.
 */
data class ToolGroup(val name: String, val tools: ObservableMap<ToolDefinition, out Tool?>)
