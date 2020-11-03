package de.uulm.se.couchedit.client.interaction.pattern

import de.uulm.se.couchedit.client.interaction.input.CanvasMouseEvent
import de.uulm.se.couchedit.client.interaction.input.OnDragHandler
import de.uulm.se.couchedit.client.util.geom.pointDistance
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import javafx.beans.property.ReadOnlyObjectWrapper
import java.lang.Double.min
import kotlin.math.abs

/**
 * Interaction pattern that allows the user to select a rectangular region.
 *
 * If the user just clicks a point, a rectangular region with that point as its upper left coordinate and the
 * [defaultSize] will be returned.
 *
 * If the user drags instead, a rectangular region will be created that has the point where the dragging ends as its
 * bottom right coordinate
 *
 * @param callback   Designates the object where the interaction should call back to when it has retrieved its results.
 *
 * @todo implement possibility to provide custom minimum width and height?
 */
class SelectRectangularRegion(
        private val defaultSize: Pair<Double, Double>,
        private val callback: InteractionPatternCallbacks<Rectangle>
) : InteractionPattern, OnDragHandler {
    private val candidateRectangleInternal = ReadOnlyObjectWrapper<Rectangular?>(null)

    val candidateRectangleProperty = candidateRectangleInternal.readOnlyProperty!!

    /**
     * Start coordinates of the drag on screen.
     */
    private var dragStartScreenCoordinates: Pair<Double, Double>? = null

    private var dragStartPoint: Point? = null

    override fun startDrag(e: CanvasMouseEvent) {
        this.dragStartPoint = e.coordinates.copy()
        this.dragStartScreenCoordinates = e.e.screenX to e.e.screenY

        this.candidateRectangleInternal.value = getRectangleFromMouseEvent(e)
    }

    override fun drag(e: CanvasMouseEvent) {
        this.candidateRectangleInternal.value = getRectangleFromMouseEvent(e)
    }

    override fun endDrag(e: CanvasMouseEvent) {
        /*
         * EndDrag seems to also take the role of onMouseUp, so we use that to handle both "click" and "drag"
         * interactions
         */
        callback.onResult(getRectangleFromMouseEvent(e))
    }

    private fun getRectangleFromMouseEvent(e: CanvasMouseEvent): Rectangle {
        val distance = dragStartScreenCoordinates?.let { (startX, startY) ->
            pointDistance(startX, startY, e.e.screenX, e.e.screenY)
        } ?: -1.0

        if (distance > MIN_DRAG_DISTANCE) {
            // If the user dragged far enough, create the rectangular area that spans the dragged area

            val x = min(dragStartPoint!!.x, e.coordinates.x)
            val y = min(dragStartPoint!!.y, e.coordinates.y)

            val w = abs(dragStartPoint!!.x - e.coordinates.x)
            val h = abs(dragStartPoint!!.y - e.coordinates.y)

            return Rectangle(x, y, w, h)
        } else {
            // In contrast, if the dragging was not far enough, just return a rectangular region with the defaultSize.
            val upperLeft = dragStartPoint ?: e.coordinates

            return Rectangle(upperLeft.x, upperLeft.y, defaultSize.first, defaultSize.second)
        }

    }

    companion object {
        /**
         * The minimum distance that must be between the [dragStartScreenCoordinates] and the drag end point for it
         * to be considered a drag action and not just a click.
         */
        const val MIN_DRAG_DISTANCE = 10.0
    }
}
