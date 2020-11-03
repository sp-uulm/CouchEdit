package de.uulm.se.couchedit.client.interaction.input

import de.uulm.se.couchedit.model.graphic.shapes.Point
import javafx.scene.input.MouseEvent

/**
 *  Wrapper for a JavaFX [MouseEvent] [e], also containing [coordinates] relative to the canvas area.
 */
data class CanvasMouseEvent(val coordinates: Point, val e: MouseEvent)
