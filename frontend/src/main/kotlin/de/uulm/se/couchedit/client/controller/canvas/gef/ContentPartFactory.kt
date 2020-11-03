package de.uulm.se.couchedit.client.controller.canvas.gef

import com.google.inject.Inject
import com.google.inject.Injector
import de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject.BendableLinePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject.LabelPart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject.RectanglePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject.RoundedRectanglePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.system.RootDrawingPart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.element.RootDrawing
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.IContentPart
import org.eclipse.gef.mvc.fx.parts.IContentPartFactory

/**
 * The [ContentPartFactory] is responsible for creating the fitting GEF [IContentPart]s for
 * any added Element.
 */
class ContentPartFactory @Inject constructor(private val injector: Injector) : IContentPartFactory {
    override fun createContentPart(content: Any?, contextMap: Map<Any, Any>): IContentPart<out Node> {
        if (content == null) {
            throw IllegalArgumentException("Content must not be null!")
        }

        if (content is RootDrawing) {
            return injector.getInstance(RootDrawingPart::class.java)
        }

        if (content is GraphicObject<*>) {
            when (content.shape) {
                is RoundedRectangle -> return injector.getInstance(RoundedRectanglePart::class.java)
                is Rectangle -> return injector.getInstance(RectanglePart::class.java)
                is StraightSegmentLine -> return injector.getInstance(BendableLinePart::class.java)
                is Label -> return injector.getInstance(LabelPart::class.java)
            }
        }

        throw IllegalArgumentException("Unknown content type <" + content.javaClass.name + ">")
    }
}
