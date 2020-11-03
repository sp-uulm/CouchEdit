package de.uulm.se.couchedit.client.controller.canvas.gef.part

import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.BaseVisual
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import javafx.scene.Node
import javafx.scene.transform.Affine
import javafx.scene.transform.Translate
import org.eclipse.gef.geometry.planar.Point
import org.eclipse.gef.mvc.fx.parts.IBendableContentPart

abstract class BaseBendablePart<V, C> : BasePart<V, C>(), IBendableContentPart<V>
        where V : Node, V : BaseVisual, C : Shape {
    private val contentAnchorages = HashMultimap.create<Any, String>()

    abstract fun setBendPoints(target: C, bendPoints: List<Point>?)

    abstract fun getStagingContentBendPoints(): MutableList<IBendableContentPart.BendPoint>

    fun setStagingContentBendPoints(bendPoints: MutableList<IBendableContentPart.BendPoint>?) {
        val oldStagingContent = stagingContent?.copy()

        this.stagingContent?.let { setBendPoints(it.shape, parentBendPointsToCanvasPoints(bendPoints ?: return)) }

        if (this.stagingContent?.equivalent(oldStagingContent) == false) {
            publishContent(true)
        }
    }

    override fun setContentBendPoints(bendPoints: MutableList<IBendableContentPart.BendPoint>?) {
        val oldContent = this.content?.copy()

        this.content?.let { setBendPoints(it.shape, parentBendPointsToCanvasPoints(bendPoints ?: return)) }

        if (this.content?.equivalent(oldContent) == false) {
            publishContent(false)
        }
    }

    override fun doGetContentAnchorages(): SetMultimap<out Any, String> = this.contentAnchorages

    override fun doAttachToContentAnchorage(contentAnchorage: Any?, role: String?) {
        this.contentAnchorages.put(contentAnchorage, role)
    }

    override fun doDetachFromContentAnchorage(contentAnchorage: Any?, role: String?) {
        this.contentAnchorages.remove(contentAnchorage, role)
    }

    /**
     * Converts a GEF [Point] instance to a [Point] with coordinates relative to the canvas, not the
     * element parent.
     */
    protected fun parentPointToCanvasPoint(point: Point): Point {
        val transform = Affine(Translate(point.x, point.y))

        val canvasCoordinateTransform = this.calculateCanvasContentCoordinates(transform)

        return Point(canvasCoordinateTransform.tx, canvasCoordinateTransform.ty)
    }

    protected fun canvasPointToParentPoint(point: Point): Point {
        val transform = Affine(Translate(point.x, point.y))

        val canvasCoordinateTransform = this.calculateParentVisualCoordinates(transform)

        return Point(canvasCoordinateTransform.tx, canvasCoordinateTransform.ty)
    }

    protected fun modelPointToGefPoint(modelPoint: de.uulm.se.couchedit.model.graphic.shapes.Point): Point = Point(modelPoint.x, modelPoint.y)

    /**
     * Converts GEF [IBendableContentPart.BendPoint]s (which are relative to the parent coordinate system to GEF [Point]s
     * relative to the canvas coordinate system
     */
    protected fun parentBendPointsToCanvasPoints(bendPoints: MutableList<IBendableContentPart.BendPoint>?) = bendPoints?.map(IBendableContentPart.BendPoint::getPosition)?.map(this::parentPointToCanvasPoint)

    companion object {
        fun getRole(isEnd: Boolean) = if (isEnd) ANCHOR_ROLE_END else ANCHOR_ROLE_START

        const val ANCHOR_ROLE_START = "start"
        const val ANCHOR_ROLE_END = "end"
    }
}
