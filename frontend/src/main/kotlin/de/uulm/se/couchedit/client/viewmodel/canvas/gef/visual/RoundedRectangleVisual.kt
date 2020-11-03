package de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual

import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import org.eclipse.gef.fx.nodes.GeometryNode
import org.eclipse.gef.geometry.planar.RoundedRectangle

/**
 * GEF / JavaFX component representing the [de.uulm.se.couchedit.model.base.primitive.Rectangle] GraphicObject.
 */
class RoundedRectangleVisual : Region(), BaseVisual {
    var arcRadius
        get() = this.geometry.arcWidth
        set(value) {
            this.geometry.arcWidth = value
            this.geometry.arcHeight = value

            this.shape.geometry = geometry
        }

    private val geometry = RoundedRectangle(0.0, 0.0, 70.0, 70.0, 0.0, 0.0)

    private val shape = GeometryNode(geometry)

    init {
        this.shape.prefWidthProperty().bind(this.widthProperty())
        this.shape.prefHeightProperty().bind(this.heightProperty())

        children.add(this.shape)
    }

    override fun addChild(visual: Node, index: Int?) {
        index?.let { this.children.add(it + 1, visual) } ?: this.children.add(visual)
    }

    override fun removeChild(visual: Node) {
        this.children.remove(visual)
    }
}
