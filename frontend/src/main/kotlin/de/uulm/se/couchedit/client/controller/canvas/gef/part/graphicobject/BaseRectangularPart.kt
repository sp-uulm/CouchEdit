package de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject

import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseTransformableResizablePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BoundHandleResizePart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.BaseVisual
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import javafx.scene.Node
import javafx.scene.transform.Affine
import javafx.scene.transform.Translate
import org.eclipse.gef.geometry.planar.Dimension

/**
 * Base Class for ContentParts which are manipulated by moving & resizing a rectangular bounding box
 * (Content class [C] must be a [Rectangular] subtype, using the x, y, w, h variables)
 */
abstract class BaseRectangularPart<V, C : Rectangular> : BaseTransformableResizablePart<V, C>(),
        BoundHandleResizePart
        where V : Node, V : BaseVisual {

    override fun getContentTransform(): Affine? = content?.let {
        /*
         * as the content always contains global coordinates
         * (so we don't have to check componentOf relations anytime we need to calculate e.g. spatial relations)
         * we need to convert them into coordinates relative to the parent's origin
         */
        calculateParentVisualCoordinates(Affine(Translate(it.shape.x, it.shape.y)))
    }

    override fun getContentSize(): Dimension? = content?.let { Dimension(it.shape.w, it.shape.h) }

    override fun setVisualSize(totalSize: Dimension) {
        super.setVisualSize(totalSize)

        visual.parent.layout()
    }

    override fun setTransform(target: C, transform: Affine) {
        // storing the new position
        target.x = transform.tx
        target.y = transform.ty
    }

    override fun setSize(target: C, size: Dimension) {
        target.w = size.width
        target.h = size.height
    }

}
