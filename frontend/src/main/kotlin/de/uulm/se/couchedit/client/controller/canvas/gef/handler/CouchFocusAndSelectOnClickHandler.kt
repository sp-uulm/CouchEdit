package de.uulm.se.couchedit.client.controller.canvas.gef.handler

import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import javafx.scene.input.MouseEvent
import org.eclipse.gef.mvc.fx.handlers.FocusAndSelectOnClickHandler

class CouchFocusAndSelectOnClickHandler : FocusAndSelectOnClickHandler() {
    private val toolModel: ToolModel by lazy {
        host.root.viewer.getAdapter(ToolModel::class.java)
    }

    override fun isFocusAndSelect(event: MouseEvent?): Boolean {
        // only execute selection action if no Tool is used (= Selection tool)
        return toolModel.selectToolEnabled && super.isFocusAndSelect(event)
    }
}
