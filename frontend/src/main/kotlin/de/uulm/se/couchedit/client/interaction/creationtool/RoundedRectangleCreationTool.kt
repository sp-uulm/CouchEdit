package de.uulm.se.couchedit.client.interaction.creationtool

import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape

class RoundedRectangleCreationTool : RectangularCreationTool() {
    override fun createShapes(givenRectangle: Rectangle): Set<Shape> {
        with(givenRectangle) {
            return setOf(RoundedRectangle(x, y, w, h, CORNER_RADIUS))
        }
    }

    companion object {
        const val CORNER_RADIUS = 15.0
    }
}
