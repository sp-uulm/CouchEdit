package de.uulm.se.couchedit.client.interaction.input

/**
 * Interface for Tools and Interaction Patterns that react on mouse clicks.
 *
 * Mirrors the methods of GEF IOnClickHandler for handling by Tools.
 */
interface OnClickHandler {
    /**
     * Method to be called when the user clicks on the canvas, with additional information about the click operation
     * executed being contained in the [e] Event.
     */
    fun onClick(e: CanvasMouseEvent) {}
}
