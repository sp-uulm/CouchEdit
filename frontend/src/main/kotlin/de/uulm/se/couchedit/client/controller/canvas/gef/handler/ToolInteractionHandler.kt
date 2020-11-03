package de.uulm.se.couchedit.client.controller.canvas.gef.handler

import de.uulm.se.couchedit.client.interaction.Tool
import de.uulm.se.couchedit.client.interaction.input.CanvasMouseEvent
import de.uulm.se.couchedit.client.util.gef.rootDrawingPart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import de.uulm.se.couchedit.model.graphic.shapes.Point
import javafx.scene.Node
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import org.eclipse.gef.geometry.planar.Dimension
import org.eclipse.gef.mvc.fx.handlers.*

/**
 * Handler that passes interactions executed on the canvas to the current tool of the [ToolModel].
 */
class ToolInteractionHandler : AbstractHandler(), IOnClickHandler, IOnHoverHandler, IOnDragHandler, IOnStrokeHandler {
    private val viewer by lazy {
        host.root.viewer
    }

    private val creationModel by lazy {
        viewer.getAdapter(ToolModel::class.java) ?: throw IllegalStateException("No ToolModel bound to viewer!")
    }

    private val rootDrawingPart by lazy { viewer.rootDrawingPart }

    override fun click(e: MouseEvent) {
        if (!e.isPrimaryButtonDown) {
            return  // wrong mouse button
        }

        withCurrentTool {
            onClick(e.convertToCanvas())
        }
    }

    override fun hoverIntent(hoverIntent: Node?) {
        // no-op. Currently unused. TODO: Maybe useful for creation of Explicit ConnectionEnds
    }

    override fun hover(e: MouseEvent) {
        withCurrentTool { onHover(e.convertToCanvas()) }
    }

    override fun initialPress(e: KeyEvent) {
        withCurrentTool { initialPress(e) }
    }

    override fun finalRelease(e: KeyEvent) {
        withCurrentTool { finalRelease(e) }
    }

    override fun press(e: KeyEvent) {
        withCurrentTool { press(e) }
    }

    override fun release(e: KeyEvent) {
        withCurrentTool { release(e) }
    }

    override fun abortPress() {
        withCurrentTool { abortPress() }
    }

    override fun abortDrag() {
        withCurrentTool { abortDrag() }
    }

    override fun endDrag(e: MouseEvent, delta: Dimension?) {
        withCurrentTool { endDrag(e.convertToCanvas()) }
    }

    override fun showIndicationCursor(event: KeyEvent?): Boolean {
        return false
    }

    override fun showIndicationCursor(event: MouseEvent?): Boolean {
        return false
    }

    override fun hideIndicationCursor() {
    }

    override fun startDrag(e: MouseEvent) {
        withCurrentTool { startDrag(e.convertToCanvas()) }
    }

    override fun drag(e: MouseEvent, delta: Dimension?) {
        withCurrentTool { drag(e.convertToCanvas()) }
    }

    private fun MouseEvent.convertToCanvas(): CanvasMouseEvent {
        val localCoords = rootDrawingPart.visual.sceneToLocal(this.sceneX, this.sceneY)

        return CanvasMouseEvent(Point(localCoords.x, localCoords.y), this)
    }

    private inline fun withCurrentTool(c: Tool.() -> Unit) {
        creationModel.tool?.apply(c)
    }
}
