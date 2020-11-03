package de.uulm.se.couchedit.client.interaction.creationtool

import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape

class RectangleCreationTool : RectangularCreationTool() {
    override fun createShapes(givenRectangle: Rectangle): Set<Shape> {
        return setOf(givenRectangle.copy())
    }
}
