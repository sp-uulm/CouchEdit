package de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual

import javafx.scene.Node
import javafx.scene.layout.Region
import org.eclipse.gef.fx.nodes.GeometryNode
import org.eclipse.gef.geometry.planar.Rectangle

/**
 * GEF / JavaFX component representing the [de.uulm.se.couchedit.model.base.primitive.Rectangle] GraphicObject.
 */
class RectangleVisual : Region(), BaseVisual {
    private val shape = GeometryNode(Rectangle(0.0, 0.0, 70.0, 70.0))

    init {
        this.shape.prefWidthProperty().bind(this.widthProperty())
        this.shape.prefHeightProperty().bind(this.heightProperty())

        //TODO
        //this.shape.fill = Color.AQUA

        children.add(this.shape)
    }

    override fun addChild(visual: Node, index: Int?) {
        index?.let { this.children.add(it + 1, visual) } ?: this.children.add(visual)
    }

    override fun removeChild(visual: Node) {
        this.children.remove(visual)
    }
}
