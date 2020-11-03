package de.uulm.se.couchedit.processing.spatial.include.stack

import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo

class StackedRoundedRectangleIncludeTest(gridSizeX: Int, gridSizeY: Int, stackDepth: Int) : StackedIncludeTest(gridSizeX, gridSizeY, stackDepth) {
    override val testInstanceInfo: TestInstanceInfo = TestInstanceInfo(
            "SRRIT",
            "StackedRoundedRectangleIncludeTest $gridSizeX x $gridSizeY; stack depth $stackDepth",
            stackedIncludeTestInfo
    )

    override fun getShapeWithBounds(x: Double, y: Double, w: Double, h: Double): Rectangular {
        return RoundedRectangle(x, y, w, h, CORNER_RADIUS)
    }

    companion object {
        const val CORNER_RADIUS = 10.0
    }
}
