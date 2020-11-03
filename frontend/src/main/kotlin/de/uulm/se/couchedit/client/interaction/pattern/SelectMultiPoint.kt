package de.uulm.se.couchedit.client.interaction.pattern

import de.uulm.se.couchedit.client.interaction.input.*
import de.uulm.se.couchedit.client.util.geom.pointDistance
import de.uulm.se.couchedit.model.graphic.shapes.Point
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton

/**
 * Interaction where the user may select a set of consecutive points.
 * This is used primarily for creating lines, but other future uses can also be incorporated.
 *
 * The user can click an arbitrary number of times to select the points, one at a time, by clicking on the canvas
 * with the left mouse button.
 * As soon as he double-clicks or uses the ENTER key, the selection is confirmed and passed back to the [callback].
 * Whenever the ESCAPE key or a mouse button different from the left one is used, the interaction stops.
 *
 * @param startPoint The first point which is to be selected in the multi-point selection process (which usually is the
 *                   point with which the tool has been activated).
 * @param callback   Designates the object where the interaction should call back to when it has retrieved its results.
 * @param enableLineDrawing This should be set to true if this interaction aims at drawing a line. If true, if the user
 *                          has started a drag gesture, ending that drag will confirm the interaction.
 *                          This is useful for quickly "drawing a line" without having to double-click.
 */
class SelectMultiPoint(
        startPoint: Point,
        private val callback: InteractionPatternCallbacks<List<Point>>,
        private val enableLineDrawing: Boolean = false
) : InteractionPattern, OnKeyStrokeHandler, OnClickHandler, OnDragHandler, OnHoverHandler {
    /**
     * List of [Point]s that the user has already clicked during the interaction.
     */
    val acceptedPointObservable = FXCollections.observableList<Point>(mutableListOf(startPoint))

    /**
     * Contains the current
     */
    val candidatePointObservable = SimpleObjectProperty<Point?>()

    /**
     * Start of a "line drawing" drag gesture in screen coordinates.
     */
    private var drawingStart: Pair<Double, Double>? = null

    override fun onHover(e: CanvasMouseEvent) {
        setCandidateFromEvent(e)
    }

    override fun onClick(e: CanvasMouseEvent) {
        if (e.e.button == MouseButton.PRIMARY) {
            if (e.e.clickCount >= 2) {
                confirm()
            } else {
                acceptedPointObservable.add(e.coordinates.copy())
            }
        } else {
            callback.onAbort()
        }
    }

    override fun drag(e: CanvasMouseEvent) {
        setCandidateFromEvent(e)
    }

    override fun startDrag(e: CanvasMouseEvent) {
        val lastPoint = acceptedPointObservable.last()

        val distance = pointDistance(lastPoint.x, lastPoint.y, e.coordinates.x, e.coordinates.y)

        if (distance < START_DRAG_MAX_DISTANCE) {
            // start line drawing
            drawingStart = e.e.screenX to e.e.screenY
        }
    }

    override fun abortDrag() {
        if (drawingStart != null) {
            callback.onAbort()
        }
    }

    override fun endDrag(e: CanvasMouseEvent) {
        val distance = drawingStart?.let { (x, y) -> pointDistance(x, y, e.e.screenX, e.e.screenY) } ?: -1.0

        if (distance > END_DRAG_MIN_DISTANCE) {
            acceptedPointObservable.add(e.coordinates.copy())

            confirm()
        }
    }

    override fun finalRelease(e: KeyEvent) {
        if (e.code == KeyCode.ESCAPE) {
            callback.onAbort()
        }

        if (e.code == KeyCode.ENTER) {
            // Accept current "candidate point", i.e. last point that was hovered.
            acceptedPointObservable.add(candidatePointObservable.value)
            confirm()
        }
    }

    private fun setCandidateFromEvent(e: CanvasMouseEvent) {
        candidatePointObservable.value = e.coordinates.copy()
    }

    private fun confirm() {
        callback.onResult(this.acceptedPointObservable.toList())
    }

    companion object {
        /**
         * The maximum distance that a dragging start may be from the last accepted point and still count as a drag to
         * draw a line (which will then be confirmed as soon as the user releases the mouse button).
         */
        const val START_DRAG_MAX_DISTANCE = 20.0

        /**
         * The minimum distance that the mouse must be dragged away from the starting point so that it counts as a drag.
         *
         * This is necessary as else, every click also counts as a drag.
         *
         * In contrast to the points that are used elsewhere in this Interaction Pattern, this uses the mouse
         * coordinates  in the window, so that normal line creation with multiple clicks can also be done at low zoom levels.
         */
        const val END_DRAG_MIN_DISTANCE = 20.0
    }
}
