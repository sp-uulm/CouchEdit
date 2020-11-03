package de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual

import javafx.scene.Group
import javafx.scene.Node

class RootVisual: Group(), SupportsChildren {
    override fun addChild(visual: Node, index: Int?) {
        index?.let { this.children.add(it, visual) } ?: this.children.add(visual)
    }

    override fun removeChild(visual: Node) {
        this.children.remove(visual)
    }
}
