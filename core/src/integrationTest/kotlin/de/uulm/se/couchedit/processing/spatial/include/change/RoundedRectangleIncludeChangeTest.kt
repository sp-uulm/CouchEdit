package de.uulm.se.couchedit.processing.spatial.include.change

import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo

class RoundedRectangleIncludeChangeTest(
        outerGridSizeX: Int,
        outerGridSizeY: Int,
        innerGridSizeX: Int,
        innerGridSizeY: Int
) : IncludeChangeTest(outerGridSizeX, outerGridSizeY, innerGridSizeX, innerGridSizeY) {
    override val testInstanceInfo = TestInstanceInfo(
            "RRICT",
            "RoundedRectangleIncludeChangeTest; Outer Grid $outerGridSizeX * $outerGridSizeY, Inner Grid $outerGridSizeX x $outerGridSizeY",
            includeChangeTestInfo
    )

    override fun getRectangularShape(x: Double, y: Double, w: Double, h: Double): Rectangular {
        return RoundedRectangle(x, y, w, h, 10.0)
    }
}
