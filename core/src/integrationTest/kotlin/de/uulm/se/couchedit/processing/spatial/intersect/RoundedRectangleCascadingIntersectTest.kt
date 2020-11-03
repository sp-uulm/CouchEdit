package de.uulm.se.couchedit.processing.spatial.intersect

import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo

class RoundedRectangleCascadingIntersectTest(
        cascadeSize: Int,
        numberOfElementsToMove: Int
) : CascadingIntersectTest(cascadeSize, numberOfElementsToMove) {
    override val testInstanceInfo = TestInstanceInfo(
            "RRCIT",
            "RoundedRectangleCascadingIntersectTest: $cascadeSize cascading Elements; move $numberOfElementsToMove",
            cascadingIntersectTestInfo
    )

    override fun getRectangularShape(x: Double, y: Double, w: Double, h: Double): Rectangular {
        return RoundedRectangle(x, y, w, h, 10.0)
    }
}
