package de.uulm.se.couchedit.processing.spatial.services.geometric

import com.google.inject.Singleton
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import org.locationtech.jts.geom.*

@Singleton
class JTSGeometryBoundsCalculator {
    fun getBoundingRectangle(g: Geometry): Rectangle {
        val envelope = g.envelope

        val ret = Rectangle()

        if (envelope is Point) {
            ret.x = envelope.x
            ret.y = envelope.y
            ret.w = 0.0
            ret.h = 0.0
        }

        if (envelope is LineString) {
            // as per the documentation, if g is a line which is parallel to an axis, a LineString with two components
            // is returned.
            val bounds = envelope.getCoordinates()

            if (bounds[0].getX() == bounds[1].getX()) {
                ret.x = bounds[0].getX()
                ret.w = 0.0

                val smallerY = Math.min(bounds[0].getY(), bounds[1].getY())
                val biggerY = Math.max(bounds[0].getY(), bounds[1].getY())

                ret.y = smallerY
                ret.h = biggerY - smallerY
            }

            if (bounds[0].getY() == bounds[1].getY()) {
                ret.y = bounds[0].getY()
                ret.h = 0.0

                val smallerX = Math.min(bounds[0].getX(), bounds[1].getX())
                val biggerX = Math.max(bounds[0].getX(), bounds[1].getX())

                ret.x = smallerX
                ret.w = biggerX - smallerX
            }
        }

        if (envelope is Polygon) {
            val edgePoints = envelope.getCoordinates()

            val topLeft = edgePoints[0]
            val bottomRight = edgePoints[2]

            ret.x = topLeft.getX()
            ret.y = topLeft.getY()
            ret.w = bottomRight.getX() - topLeft.getX()
            ret.h = bottomRight.getY() - topLeft.getY()
        }

        return ret
    }

}
