package de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback.interaction

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import org.eclipse.gef.fx.anchors.StaticAnchor
import org.eclipse.gef.geometry.planar.Point

class MousePositionAnchor(referencePositionInScene: Point): StaticAnchor(referencePositionInScene), EventHandler<MouseEvent> {
    override fun handle(event: MouseEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
