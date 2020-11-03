package de.uulm.se.couchedit.client.interaction.input

/**
 * Interface for Tools and Interaction Patterns that react on mouse hovering, i.e. moving the mouse on the canvas
 * without using the buttons.
 *
 *  Mirrors the methods of the GEF IOnHoverHandler for use in Tools and InteractionPatterns.
 */
interface OnHoverHandler {
    fun onHover(e: CanvasMouseEvent) {}
}
