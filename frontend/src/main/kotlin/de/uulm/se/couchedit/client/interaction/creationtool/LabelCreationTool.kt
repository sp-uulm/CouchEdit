package de.uulm.se.couchedit.client.interaction.creationtool

import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape

class LabelCreationTool : RectangularCreationTool() {
    override fun createShapes(givenRectangle: Rectangle): Set<Shape> {
        with(givenRectangle) {
            return setOf(Label(x, y, w, h, DEFAULT_TEXT))
        }
    }

    companion object {
        const val DEFAULT_TEXT = "Text"
    }
}
