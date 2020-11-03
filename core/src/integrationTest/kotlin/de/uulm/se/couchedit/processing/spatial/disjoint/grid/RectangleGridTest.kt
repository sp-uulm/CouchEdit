package de.uulm.se.couchedit.processing.spatial.disjoint.grid

import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo

class RectangleGridTest(gridSizeX: Int, gridSizeY: Int, numberOfColumnsToMove: Int) : GridTest(gridSizeX, gridSizeY, numberOfColumnsToMove) {
    override val testInstanceInfo = TestInstanceInfo(
            "RectGridTest",
            "RectangleGridTest ${gridSizeX}x${gridSizeY}; moving $numberOfColumnsToMove elements",
            gridTestInfo
    )

    override fun getRectangularShape(x: Double, y: Double, w: Double, h: Double): Rectangular {
        return Rectangle(x, y, w, h)
    }
}
