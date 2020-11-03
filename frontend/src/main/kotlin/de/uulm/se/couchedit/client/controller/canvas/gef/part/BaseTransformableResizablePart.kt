package de.uulm.se.couchedit.client.controller.canvas.gef.part

import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.BaseVisual
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import javafx.scene.Node
import javafx.scene.transform.Affine
import org.eclipse.gef.geometry.planar.Dimension
import org.eclipse.gef.mvc.fx.parts.IResizableContentPart
import org.eclipse.gef.mvc.fx.parts.ITransformableContentPart

/**
 * [V] is the class of visual managed by this Part, [C] is the model object.
 */
abstract class BaseTransformableResizablePart<V, C : Shape> :
        BasePart<V, C>(),
        ITransformableContentPart<V>,
        IResizableContentPart<V>
        where V : Node, V : BaseVisual {

    /**
     * Refreshes the [visual] of this part based on the information from the [getContent].
     */
    override fun doRefreshVisual(visual: V?) {
        visualSize = contentSize ?: Dimension(0.0, 0.0)
        visualTransform = contentTransform ?: Affine()
    }

    /**
     * Sets the transform / position of an object of the model type [C] of this Part according to the input [transform].
     */
    abstract fun setTransform(target: C, transform: Affine)

    /**
     * Sets the size of an object of the model type [C] of this Part
     */
    abstract fun setSize(target: C, size: Dimension)

    override fun doUpdateContentInstance(content: C) {
        this.setTransform(content, calculateCanvasContentCoordinates(visualTransform))
        this.setSize(content, visualSize)
    }

    /**
     * Called whenever the user "commits" / completes a transaction which will have effect on the position of the
     * associated content of this Part.
     *
     * In the case of the CouchEdit application, this method will refresh the properties of the "main" (not staging)
     * content.
     */
    override fun setContentTransform(totalTransform: Affine) {
        val elements = this.getDependantPartsWithContent(true)

        // ensure that the contents correspond to their visual
        elements.forEach { (part, _) -> part.updateContentFromVisual(false) }

        this.content?.let { this.setTransform(it.shape, this.calculateCanvasContentCoordinates(totalTransform)) }

        this.publishContent(false)
    }

    /**
     * Called whenever the user "commits" / completes a transaction which will have effect on the size associated content
     * of this Part.
     *
     * In the case of the CouchEdit application, this method will refresh the properties of the "main" (not staging)
     * content.
     */
    override fun setContentSize(totalSize: Dimension) {
        this.content?.let { this.setSize(it.shape, totalSize) }

        this.publishContent(false)
    }

    /**
     * Called whenever the [visual]'s transform needs to be changed.
     *
     * This is called when the user moves the element. Furthermore, it is called by [doRefreshVisual] and should be
     * called by all of its overrides, as it has to be ensured that the visual representation is always in sync with
     * the Content.
     *
     * To also track live changes to the elements (as GEF usually only updates the content after the user lets go)
     * [updateContentFromVisual] is called here for the [stagingContent], then the content is published to the rest
     * of the application.
     */
    override fun setVisualTransform(totalTransform: Affine?) {
        totalTransform ?: return
        super.setVisualTransform(totalTransform)

        updateContentFromVisual(true)
        this.publishContent(true)

        visual.parent.layout()
    }

    /**
     * Called whenever the [visual]'s size needs to be changed.
     *
     * This is called when the user resizes the element. Furthermore, it is called by [doRefreshVisual] and should be
     * called by all of its overrides, as it has to be ensured that the visual representation is always in sync with
     * the Content.
     */
    override fun setVisualSize(totalSize: Dimension) {
        super.setVisualSize(totalSize)

        stagingContent?.let {
            this.setSize(it.shape, totalSize)
        }

        this.publishContent(true)
    }
}
