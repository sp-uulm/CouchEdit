package de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject

import de.uulm.se.couchedit.client.controller.canvas.gef.part.BoundHandleResizePart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.RoundedRectangleVisual
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle

class RoundedRectanglePart : BaseRectangularPart<RoundedRectangleVisual, RoundedRectangle>(),
        BoundHandleResizePart {
    override fun doCreateVisual() = RoundedRectangleVisual()

    override fun doRefreshVisual(visual: RoundedRectangleVisual?) {
        super.doRefreshVisual(visual)

        visual?.let { it.arcRadius = this.content!!.shape.cornerRadius }
    }
}
