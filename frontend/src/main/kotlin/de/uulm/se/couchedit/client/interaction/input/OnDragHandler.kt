package de.uulm.se.couchedit.client.interaction.input

/**
 * Interface for tools and interaction patterns that react to drag operations ecevuted on the canvas.
 *
 * Mirrors the methods of the GEF IOnDragHandler for use in Tools.
 */
interface OnDragHandler {
    /**
     * Called when a mouse button is pressed, starting a drag action.
     */
    fun startDrag(e: CanvasMouseEvent) {}

    /**
     * Called when the mouse button is released after dragging, ending the drag operation.
     */
    fun endDrag(e: CanvasMouseEvent) {}

    /**
     * Called when a drag action is cancelled unexpectedly, for example by the mouse cursor leaving the window.
     */
    fun abortDrag() {}

    /**
     * Invoked during the drag option when the mouse is moved.
     */
    fun drag(e: CanvasMouseEvent) {}
}
