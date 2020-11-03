package de.uulm.se.couchedit.processing.spatial.services.geometric

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.operation.distance.DistanceOp
import java.lang.Math.pow
import kotlin.math.sqrt

@Singleton
class NearestPointCalculator @Inject constructor(private val geometryProvider: JTSGeometryProvider) {
    fun isDistanceUnder(
            s1: Shape,
            s2: Shape,
            threshold: Double,
            s1ElementId: String? = null,
            s2ElementId: String? = null
    ): Boolean {
        val envDistance = this.calculateEnvelopeDistanceBetween(s1, s2, s1ElementId, s2ElementId)

        if (envDistance > threshold) {
            return false
        }

        val realDistance = calculateDistanceBetween(s1, s2, s1ElementId, s2ElementId)

        return realDistance < threshold
    }

    fun calculateEnvelopeDistanceBetween(
            s1: Shape,
            s2: Shape,
            s1ElementId: String? = null,
            s2ElementId: String? = null
    ): Double {
        val (p1, p2) = calculateNearestEnvelopePointsBetween(s1, s2, s1ElementId, s2ElementId)

        val dx = p1.x - p2.x
        val dy = p1.y - p2.y

        return sqrt(pow(dx, 2.0) + pow(dy, 2.0))
    }

    fun calculateNearestEnvelopePointsBetween(
            s1: Shape,
            s2: Shape,
            s1ElementId: String? = null,
            s2ElementId: String? = null
    ): Pair<Point, Point> {
        val envS1 = geometryProvider.toGeometry(s1, s1ElementId).geometry.envelopeInternal
        val envS2 = geometryProvider.toGeometry(s2, s2ElementId).geometry.envelopeInternal

        val x1: Double
        val x2: Double
        val y1: Double
        val y2: Double

        when {
            envS1.minX > envS2.maxX -> {
                x1 = envS1.minX
                x2 = envS2.maxX
            }
            envS2.minX > envS1.maxX -> {
                x1 = envS1.maxX
                x2 = envS2.minX
            }
            else -> {
                x1 = envS1.minX
                x2 = x1
            }
        }

        when {
            envS1.minY > envS2.maxY -> {
                y1 = envS1.minY
                y2 = envS2.maxY
            }
            envS2.minY > envS1.maxY -> {
                y1 = envS1.maxY
                y2 = envS2.minY
            }
            else -> {
                y1 = envS1.minY
                y2 = y1
            }
        }

        return Pair(Point(x1, y1), Point(x2, y2))
    }

    fun calculateNearestPointsBetween(
            s1: Shape,
            s2: Shape,
            s1ElementId: String? = null,
            s2ElementId: String? = null
    ): Pair<Point, Point> {
        val g1 = geometryProvider.toGeometry(s1, s1ElementId)
        val g2 = geometryProvider.toGeometry(s2, s2ElementId)

        val distanceOp = DistanceOp(g1.geometry, g2.geometry)

        val coordinates = distanceOp.nearestPoints()

        return Pair(coordinateToPoint(coordinates[0]), coordinateToPoint(coordinates[1]))
    }

    fun calculateDistanceBetween(
            s1: Shape,
            s2: Shape,
            s1ElementId: String? = null,
            s2ElementId: String? = null
    ): Double {
        val g1 = geometryProvider.toGeometry(s1, s1ElementId)
        val g2 = geometryProvider.toGeometry(s2, s2ElementId)

        val distanceOp = DistanceOp(g1.geometry, g2.geometry)

        return distanceOp.distance()
    }

    fun calculateBorderDistanceBetween(
            s1: Shape,
            s2: Shape,
            s1ElementId: String? = null,
            s2ElementId: String? = null
    ) : Double {
        val g1 = geometryProvider.toGeometry(s1, s1ElementId)
        val g2 = geometryProvider.toGeometry(s2, s2ElementId)

        val distanceOp = DistanceOp(
                if(s1 is Point) g1.geometry else g1.geometry.boundary,
                if(s2 is Point) g2.geometry else g2.geometry.boundary
        )

        return distanceOp.distance()
    }

    private fun coordinateToPoint(coordinate: Coordinate): Point {
        return Point(coordinate.x, coordinate.y)
    }
}
