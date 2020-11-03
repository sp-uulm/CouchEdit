package de.uulm.se.couchedit.processing.spatial.services.geometric

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon

/**
 * Service to convert JTS Geometries back to Shape objects (counterpart to [JTSGeometryProvider])
 */
@Singleton
class ShapeProvider @Inject constructor(
        private val geometryBoundsCalculator: JTSGeometryBoundsCalculator
) {
    /**
     * Converts the given [geometry] to a [Shape].
     */
    fun toShape(geometry: Geometry): Shape {
        when (geometry) {
            is Point -> return Point(geometry.x, geometry.y)
            is Polygon -> {
                if (geometry.isRectangle) {
                    return geometryBoundsCalculator.getBoundingRectangle(geometry)
                }

                val holeList = mutableListOf<List<Point>>()

                for (i in 0 until geometry.numInteriorRing) {
                    holeList.add(coordinateSequenceToPointList(geometry.getInteriorRingN(i).coordinateSequence))
                }

                return de.uulm.se.couchedit.model.graphic.shapes.Polygon(
                        coordinateSequenceToPointList(geometry.exteriorRing.coordinateSequence),
                        holeList
                )
            }
            is LineString -> {
                return StraightSegmentLine(coordinateSequenceToPointList(geometry.coordinateSequence))
            }
            else -> {
                throw IllegalArgumentException("ShapeProvider: Type ${geometry::class.java} is not supported!")
            }
        }

    }

    companion object {
        fun coordinateSequenceToPointList(coordinateSequence: CoordinateSequence): List<Point> {
            val ret = mutableListOf<Point>()

            for (i in 0 until coordinateSequence.size()) {
                val coordinate = coordinateSequence.getCoordinate(i)

                ret.add(Point(coordinate.x, coordinate.y))
            }

            return ret.toList()
        }
    }
}
