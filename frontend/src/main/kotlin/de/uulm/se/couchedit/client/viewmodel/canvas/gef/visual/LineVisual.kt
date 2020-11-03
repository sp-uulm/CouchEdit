package de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual

import javafx.scene.Node
import org.eclipse.gef.fx.nodes.Connection

import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Line
import javafx.scene.shape.Path
import javafx.scene.shape.Polygon
import org.eclipse.gef.fx.nodes.GeometryNode

class LineVisual : Connection(), BaseVisual {
    var stroke: Paint
        get() = line.stroke
        set(value) {
            line.stroke = value
        }

    var strokeDashList
        get() = line.strokeDashArray.toList()
        set(value) {
            line.strokeDashArray.setAll(value)
        }

    private val line: Path
        get() = (this.curve as GeometryNode<*>).path as Path

    override fun addChild(visual: Node, index: Int?) {
        index?.let { this.children.add(it, visual) } ?: this.children.add(visual)
    }

    override fun removeChild(visual: Node) {
        this.children.remove(visual)
    }
}
