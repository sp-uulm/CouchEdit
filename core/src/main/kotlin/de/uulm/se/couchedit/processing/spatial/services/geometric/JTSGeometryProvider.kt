package de.uulm.se.couchedit.processing.spatial.services.geometric

import com.google.common.collect.HashMultimap
import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.graphic.shapes.*
import de.uulm.se.couchedit.util.collection.TTLMap
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.prep.PreparedGeometry
import org.locationtech.jts.geom.prep.PreparedGeometryFactory
import org.locationtech.jts.util.GeometricShapeFactory
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Converts [Shape]s into Java Topology Suite [PreparedGeometry]s.
 *
 * Shapes are cached by their Hash and optionally a key that is associated with the shape.
 */
@Singleton
class JTSGeometryProvider @Inject constructor(private val preparedGeometryFactory: PreparedGeometryFactory) {
    private val geometries = TTLMap<Int, PreparedGeometry>()

    private val knownShapes = HashMap<String, Int>()

    private val knownIds = HashMultimap.create<Int, String>()

    /**
     * Converts the given shape into a JTS [PreparedGeometry].
     *
     * The given [objectId] is optionally used for caching calculated values,
     * if a Shape with a new HashCode is given for an already known [Geometry],
     * the old Shape will be removed from the cache.
     */
    fun toGeometry(shape: Shape, objectId: String? = null): PreparedGeometry {
        val gsf = GeometricShapeFactory()
        val gf = GeometryFactory()

        val hash = shape.hashCode()

        geometries[hash]?.let { return it }

        // If we know an older shape for this object ID and no other IDs are using it, delete it
        objectId?.let { id ->
            knownShapes.remove(id)?.let { oldShapeHash ->
                knownIds.remove(oldShapeHash, id)

                if (!knownIds.containsKey(oldShapeHash)) {
                    geometries.remove(oldShapeHash)
                }
            }

        }

        val geom = when (shape) {
            is RoundedRectangle -> {
                gf.createPolygon(
                        pointListToCoordinateArray(this.generateRoundedRectPoints(shape))
                )
            }
            is Rectangular -> {
                gsf.setHeight(shape.h)
                gsf.setWidth(shape.w)
                gsf.setBase(Coordinate(shape.x, shape.y))

                gsf.createRectangle()
            }
            is StraightSegmentLine -> {
                gf.createLineString(pointListToCoordinateArray(shape.points))
            }
            is Point -> {
                gf.createPoint(Coordinate(shape.x, shape.y))
            }
            is Polygon -> {
                gf.createPolygon(
                        gf.createLinearRing(pointListToCoordinateArray(shape.outerBorder)),
                        shape.holes.map { gf.createLinearRing(pointListToCoordinateArray(it)) }.toTypedArray()
                )
            }
            else -> throw RuntimeException("JTSGeometryProvider has no implementation for " + shape.javaClass)
        }

        return preparedGeometryFactory.create(geom)
    }

    /**
     * Generates a List of Points that define a RoundedRectangle as given by [shape].
     * The number of sampling points for the corner is equal to the [RoundedRectangle.cornerRadius].
     *
     * The CornerRadius is handled similarly to the
     */
    private fun generateRoundedRectPoints(shape: RoundedRectangle): List<Point> {
        val effectiveCornerRadiusX = min(shape.cornerRadius, shape.w / 2)
        val effectiveCornerRadiusY = min(shape.cornerRadius, shape.h / 2)

        val sampleSize = (shape.cornerRadius * CURVE_SAMPLE_FACTOR).roundToInt()

        // start with top right corner
        val startTr = Point(shape.x + shape.w - effectiveCornerRadiusX, shape.y)
        val controlTr = Point(shape.x + shape.w, shape.y)
        val endTr = Point(shape.x + shape.w, shape.y + effectiveCornerRadiusY)

        val ret = mutableListOf(startTr)
        ret.addAll(quadraticBezierSample(startTr, controlTr, endTr, sampleSize))

        // bottom right corner
        val startBr = Point(shape.x + shape.w, shape.y + shape.h - effectiveCornerRadiusY)
        val controlBr = Point(shape.x + shape.w, shape.y + shape.h)
        val endBr = Point(shape.x + shape.w - effectiveCornerRadiusX, shape.y + shape.h)

        ret.addAll(quadraticBezierSample(startBr, controlBr, endBr, sampleSize))

        // bottom left corner
        val startBl = Point(shape.x + effectiveCornerRadiusX, shape.y + shape.h)
        val controlBl = Point(shape.x, shape.y + shape.h)
        val endBl = Point(shape.x, shape.y + shape.h - effectiveCornerRadiusY)

        ret.addAll(quadraticBezierSample(startBl, controlBl, endBl, sampleSize))

        // top left corner
        val startTl = Point(shape.x, shape.y + effectiveCornerRadiusY)
        val controlTl = Point(shape.x, shape.y)
        val endTl = Point(shape.x + effectiveCornerRadiusX, shape.y)

        ret.addAll(quadraticBezierSample(startTl, controlTl, endTl, sampleSize))

        // close the polygon
        ret.add(startTr)

        return ret
    }

    /**
     * Generates a list of [Point]s with size = [sampleSize] that describe a Bezier curve from Point [p0] to point [p2]
     * via the control point [p1]
     */
    private fun quadraticBezierSample(p0: Point, p1: Point, p2: Point, sampleSize: Int): List<Point> {
        val ret = mutableListOf<Point>()
        for (factor in 0..sampleSize) {
            ret.add(this.generateBezier(p0, p1, p2, factor.toDouble() / sampleSize.toDouble()))
        }

        return ret
    }

    /**
     * Calculates the point of the Bezier curve from [p0] over [p1] to [p2] at ratio [t].
     *
     * For more information about this calculation, see
     * https://www.toptal.com/c-plus-plus/rounded-corners-bezier-curves-qpainter .
     */
    private fun generateBezier(p0: Point, p1: Point, p2: Point, t: Double): Point {
        return p0 * ((1 - t).pow(2)) + p1 * (2 * t * (1 - t)) + p2 * t.pow(2)
    }

    public fun removeStaleItems() {
        this.geometries.cleanUp()
    }

    companion object {
        fun pointListToCoordinateArray(points: List<Point>): Array<Coordinate> {
            return points.map { Coordinate(it.x, it.y) }.toTypedArray()
        }

        /**
         * Factor by which the dimension of a curve will be multiplied to get the number of points into which it will
         * be sampled.
         */
        const val CURVE_SAMPLE_FACTOR = 0.5
    }
}
