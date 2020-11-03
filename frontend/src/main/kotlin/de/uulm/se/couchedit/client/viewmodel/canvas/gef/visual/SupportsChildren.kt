package de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual

import javafx.scene.Node

interface SupportsChildren {
    fun addChild(visual: Node, index: Int? = null)

    fun removeChild(visual: Node)
}
