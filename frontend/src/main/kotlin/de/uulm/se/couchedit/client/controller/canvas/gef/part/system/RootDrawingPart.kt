package de.uulm.se.couchedit.client.controller.canvas.gef.part.system

import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.element.RootDrawing
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.RootVisual
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.IVisualPart

/**
 *
 * The [RootDrawingPart] is responsible to create a visual for the
 * [RootDrawing] and managing all other objects in the current drawing.
 *
 */
class RootDrawingPart : ChildrenSupportingPart<RootVisual>() {
    /**
     * Adds visual from Part child
     */
    override fun doAddChildVisual(child: IVisualPart<out Node>, index: Int) {
        visual.addChild(child.visual, index)
    }

    override fun doCreateVisual(): RootVisual {
        // the visual is just a container for our child visual (nodes and connections)
        return RootVisual()
    }

    override fun doGetContentAnchorages(): SetMultimap<out Any, String> {
        return HashMultimap.create()
    }

    override fun doRefreshVisual(visual: RootVisual) {
        // no refreshing necessary, just a Group
    }

    override fun doRemoveChildVisual(child: IVisualPart<out Node>, index: Int) {
        visual.children.remove(child.visual)
    }

    override fun getContent(): RootDrawing {
        return super.getContent() as RootDrawing
    }
}
