package de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject

import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseTransformableResizablePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BoundHandleResizePart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.RectangleVisual
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import javafx.scene.transform.Affine
import javafx.scene.transform.Translate
import org.eclipse.gef.geometry.planar.Dimension

class RectanglePart : BaseRectangularPart<RectangleVisual, Rectangle>(),
        BoundHandleResizePart {
    override fun doCreateVisual(): RectangleVisual = RectangleVisual()
}
