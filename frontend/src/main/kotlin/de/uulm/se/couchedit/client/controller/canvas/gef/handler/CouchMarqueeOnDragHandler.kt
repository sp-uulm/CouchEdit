package de.uulm.se.couchedit.client.controller.canvas.gef.handler

import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import javafx.scene.input.MouseEvent
import org.eclipse.gef.mvc.fx.handlers.MarqueeOnDragHandler

class CouchMarqueeOnDragHandler : MarqueeOnDragHandler() {
    private val toolModel: ToolModel by lazy {
        host.root.viewer.getAdapter(ToolModel::class.java)
    }

    override fun isMarquee(event: MouseEvent?): Boolean {
        // Only create marquee selection if the selection tool is enabled
        return toolModel.selectToolEnabled && super.isMarquee(event)
    }
}
