package de.uulm.se.couchedit.processing.spatial.disjoint.grid

import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo

class RoundedRectangleGridTest(gridSizeX: Int, gridSizeY: Int, numberOfColumnsToMove: Int) : GridTest(gridSizeX, gridSizeY, numberOfColumnsToMove) {
    override val testInstanceInfo = TestInstanceInfo(
            "RoundedRectGridTest",
            "RoundedRectangleGridTest ${gridSizeX}x${gridSizeY}; moving $numberOfColumnsToMove elements",
            gridTestInfo
    )

    override fun getRectangularShape(x: Double, y: Double, w: Double, h: Double): Rectangular {
        return RoundedRectangle(x, y, w, h, 10.0)
    }
}
