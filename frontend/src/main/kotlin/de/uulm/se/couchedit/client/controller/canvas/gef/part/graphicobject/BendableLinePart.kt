package de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject

import com.google.common.reflect.TypeToken
import com.google.inject.Inject
import com.google.inject.Provider
import de.uulm.se.couchedit.client.controller.canvas.fx.attributes.LineStyleConverter
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseBendablePart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.LineVisual
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineEndPointStyle
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import javafx.scene.Node
import org.eclipse.gef.fx.anchors.DynamicAnchor
import org.eclipse.gef.fx.anchors.IAnchor
import org.eclipse.gef.fx.anchors.StaticAnchor
import org.eclipse.gef.mvc.fx.parts.IBendableContentPart
import org.eclipse.gef.mvc.fx.parts.IVisualPart

internal class BendableLinePart @Inject constructor(private val lineStyleConverter: LineStyleConverter)
    : BaseBendablePart<LineVisual, StraightSegmentLine>() {

    override fun doCreateVisual(): LineVisual = LineVisual()

    override fun doRefreshVisual(visual: LineVisual?) {
        visual ?: return

        var dynamicStart = false
        var dynamicEnd = false

        if (visual.startAnchor is DynamicAnchor) {
            dynamicStart = true
        }

        if (visual.endAnchor is DynamicAnchor) {
            dynamicEnd = true
        }

        if (!dynamicStart && !dynamicEnd) {
            visual.setPoints(this.contentBendPoints.map(IBendableContentPart.BendPoint::getPosition))

            return
        }

        // have to use the StaticAnchor constructor with this.visual because else the position will be interpreted
        // in local coordinates.
        val anchors = this.contentBendPoints.map(IBendableContentPart.BendPoint::getPosition).map { StaticAnchor(this.visual, it) }.toMutableList<IAnchor>()

        // don't overwrite dynamic anchors
        if (dynamicStart) {
            anchors[0] = visual.startAnchor
        }

        if (dynamicEnd) {
            anchors[anchors.size - 1] = visual.endAnchor
        }

        this.visual.setAnchors(anchors)
    }

    override fun doRefreshAttributes() {
        val lineStartPointAttr = this.getAttributeValueFromBags(
                LineAttributes::class.java,
                GraphicAttributeKeys.LINE_START_STYLE
        ) ?: LineEndPointStyle()

        val lineEndPointAttr = this.getAttributeValueFromBags(
                LineAttributes::class.java,
                GraphicAttributeKeys.LINE_END_STYLE
        ) ?: LineEndPointStyle()

        val lineStyleAttr = this.getAttributeValueFromBags(
                LineAttributes::class.java,
                GraphicAttributeKeys.LINE_STYLE
        ) ?: LineStyle()

        this.visual.startDecoration = this.lineStyleConverter.getFXShapeForEndPointStyle(lineStartPointAttr)
        this.visual.endDecoration = this.lineStyleConverter.getFXShapeForEndPointStyle(lineEndPointAttr)

        this.visual.strokeDashList = this.lineStyleConverter.getFXStrokeDashStyleForLineStyle(lineStyleAttr)
    }

    override fun getContentBendPoints(): MutableList<IBendableContentPart.BendPoint> = getBendPointsFromContent(this.content?.shape)

    override fun getStagingContentBendPoints(): MutableList<IBendableContentPart.BendPoint> = getBendPointsFromContent(this.stagingContent?.shape)

    override fun setBendPoints(
            target: StraightSegmentLine,
            bendPoints: List<org.eclipse.gef.geometry.planar.Point>?
    ) {
        target.points = bendPoints?.mapIndexed { index, bendPoint ->
            Point(bendPoint.x, bendPoint.y)
        }?.toMutableList() ?: return

    }

    override fun doAttachToAnchorageVisual(anchorage: IVisualPart<out Node>?, role: String?) {
        anchorage ?: return

        val anchorProvider = anchorage.getAdapter(object : TypeToken<Provider<out IAnchor>>() {})

        anchorProvider?.get()?.let {
            if (role == ANCHOR_ROLE_START) {
                visual.startAnchor = it
            } else if (role == ANCHOR_ROLE_END) {
                visual.endAnchor = it
            }
        }

        this.contentBendPoints = this.visualBendPoints
    }

    override fun doDetachFromAnchorageVisual(anchorage: IVisualPart<out Node>?, role: String?) {
        if (role == ANCHOR_ROLE_START) {
            visual.startPoint = visual.startPoint
        } else if (role == ANCHOR_ROLE_END) {
            visual.endPoint = visual.endPoint
        }
    }

    override fun doUpdateContentInstance(content: StraightSegmentLine) {
        setBendPoints(content, visual.pointsUnmodifiable.map(this::parentPointToCanvasPoint))
    }

    fun getBendPointsFromContent(line: StraightSegmentLine?): MutableList<IBendableContentPart.BendPoint> {
        return line?.points?.map(this::modelPointToGefPoint)?.map(this::canvasPointToParentPoint)?.map {
            IBendableContentPart.BendPoint(org.eclipse.gef.geometry.planar.Point(it.x, it.y))
        }?.toMutableList() ?: mutableListOf()
    }
}
