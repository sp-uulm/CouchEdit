package de.uulm.se.couchedit.processing.compartment.services

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.HashMultimap
import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.jtsextensions.overlay.PolygonOverlayFunctions
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentIndex
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryBoundsCalculator
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.util.extensions.ref
import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance
import org.locationtech.jts.densify.Densifier
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.distance.DistanceOp
import kotlin.Comparator
import kotlin.math.abs

@Singleton
/**
 * Helper service that generates the geometries resulting from splitting a shape by one or more Line GraphicObjects.
 */
class CompartmentGeometryGenerator @Inject constructor(
        private val jtsGeometryProvider: JTSGeometryProvider,
        private val shapeExtractor: ShapeExtractor,
        private val geometryBoundsProvider: JTSGeometryBoundsCalculator,
        private val compartmentCache: CompartmentCache,
        private val geoFac: GeometryFactory
) {
    /**
     * Generates a [Result] object containing the [Geometry] objects resulting from splitting [baseElement]'s shape
     * by the given [lines].
     *
     * @see AREA_THRESHOLD - Minimum area that a Compartment must contain in order to be considered.
     */
    fun generateGeometries(
            baseElement: ElementReference<ShapedElement<*>>,
            related: Map<ElementReference<*>, Element>,
            lines: Collection<GraphicObject<*>>
    ): Result {
        // prevent calculating this multiple times in parallel todo per-object locking
        synchronized(this) {
            val baseShape = shapeExtractor.extractShape(baseElement.asType<ShapedElement<Shape>>(), related)
                    ?: return emptyResult()

            val baseGeometry = jtsGeometryProvider.toGeometry(baseShape, baseElement.id).geometry.let {
                Densifier.densify(it, DENSIFY_DISTANCE)
            }

            val lineGeometries = mutableMapOf<ElementReference<GraphicObject<*>>, LineString>()
            val lineGeometriesReverse = HashMultimap.create<LineString, ElementReference<GraphicObject<*>>>()

            for (line in lines) {
                (jtsGeometryProvider.toGeometry(line.shape, line.id).geometry as? LineString)?.let {
                    lineGeometries.put(line.ref(), it)
                    lineGeometriesReverse.put(it, line.ref())
                }
            }

            // if we have already cached a set of geometries for this baseshape-line combination, take that
            compartmentCache.retrieve(baseGeometry, lineGeometriesReverse)?.let { return it }

            val splittingLines = this.getSplittingLines(baseGeometry, lineGeometries)

            if (splittingLines.isEmpty()) {
                return compartmentCache.storeAndReturn(baseGeometry, lineGeometries, emptyResult())
            }

            val geometryCollection = GeometryCollection(splittingLines.values.flatten().toTypedArray(), geoFac)

            val mergedGeometry = try {
                geometryCollection.union()
            } catch (e: TopologyException) {
                System.err.println("Error while merging lines: $e")
                return emptyResult()
            }

            val resultingGeometries = try {
                PolygonOverlayFunctions.overlaySnapRounded(baseGeometry, mergedGeometry, OVERLAY_SNAP_PRECISION)
            } catch (e: TopologyException) {
                System.err.println("Compartment Polygons could not be calculated: $e")
                return emptyResult()
            }

            val builder = ResultBuilder()

            for (i in 0 until resultingGeometries.numGeometries) {
                val geometry = resultingGeometries.getGeometryN(i)

                val geomArea = geometry.area

                // Exclude Compartments with too small areas
                if (geomArea < AREA_THRESHOLD) {
                    continue
                }

                // Exclude Compartments with too small areas (and big compartments that do not leave more space in the
                // baseShape for others than the threshold)
                if (abs(baseGeometry.area - geomArea) < AREA_THRESHOLD) {
                    continue
                }

                val boundary = geometryBoundsProvider.getBoundingRectangle(geometry)

                builder.add(geometry, boundary)
            }

            val result = builder.build(splittingLines.keys)

            return compartmentCache.storeAndReturn(baseGeometry, lineGeometries, result)
        }
    }

    /**
     * Calculates the [LineString] objects that can be used to overlay on the given [baseGeometry] and split the
     * object by them.
     *
     * The [PolygonOverlayFunctions] require that these lines must
     * * Contain at least two points exactly on the border of the [baseGeometry]
     * * Only contain points that are inside the interior of the [baseGeometry]
     *
     * From a given [LineString], the relevant splitting line geometry is calculated as follows:
     * * From the beginning of the line to its end, find points that are near enough to the boundary of the given
     *   [baseGeometry]
     * * If a point is near enough, add it and all points that came between the last relevant point and this one to the
     *   splitting LineString
     * * If the point is not exactly on the boundary, also add the nearest point on the boundary
     *
     * @return Map of given [GraphicObject] ID from [lineGeometries] to JTS [LineString]s that are a valid splitter of [baseGeometry]
     *
     * @see DISTANCE_THRESHOLD - Maximum distance that a line must have from the [baseGeometry] in order to be
     *                           considered a splitting line
     */
    private fun getSplittingLines(
            baseGeometry: Geometry,
            lineGeometries: Map<ElementReference<GraphicObject<*>>, LineString>
    ): Map<ElementReference<GraphicObject<*>>, Set<LineString>> {
        val ret = mutableMapOf<ElementReference<GraphicObject<*>>, Set<LineString>>()

        val jtsBaseBoundary = baseGeometry.boundary

        lineLoop@ for ((ref, lineGeometry) in lineGeometries) {
            // get the parts of the line that are inside the baseShape
            val intersectingLineGeometry = try {
                lineGeometry.intersection(baseGeometry)
            } catch (e: TopologyException) {
                print("Topology Exception ${e.message}")

                continue
            }

            if (intersectingLineGeometry.isEmpty) {
                continue
            }

            val intersectingMultiLineString = when (intersectingLineGeometry) {
                is MultiLineString -> intersectingLineGeometry
                is LineString -> MultiLineString(arrayOf(intersectingLineGeometry), geoFac)
                // no sensible intersection returned
                else -> continue@lineLoop
            }

            if (!DistanceOp.isWithinDistance(jtsBaseBoundary, intersectingLineGeometry, DISTANCE_THRESHOLD)) {
                continue
            }

            val lineStringSet = mutableSetOf<LineString>()

            for (i in 0 until intersectingMultiLineString.numGeometries) {
                val lineCoordinates = (intersectingMultiLineString.getGeometryN(i) as LineString).coordinates

                // Saves the coordinates that are relevant to the lineString.
                val coordinates = mutableListOf<Point>()

                // saves the coordinates between relevant ones. We don't want a "tail" hanging off the actually
                // relevant coordinates, so save them in a stack and append whenever we find a fitting coordinate
                val coordinateStack = mutableListOf<Point>()

                /*
                 * This is done to address a shortcoming in JTS's algorithms.
                 *
                 * If lines are too close together in the OverlaySnapRounded input, TopologyExceptions are thrown.
                 * https://github.com/locationtech/jts/issues/120
                 *
                 * Thus, throw away lines that are at no point further away from the boundary than a threshold.
                 *
                 * This primarily happens because OverlaySnapRounded by itself is not exact, either. So it can be that
                 * the (previous) split line is just inside the result polygon's boundary, thus creating such extremely
                 * narrow line intersections that JTS can't handle.
                 * Therefore, check whether an explicit coordinate has a big enough distance from the boundary.
                 * If not, check the Haussdorf distance of the resulting (connected) LineString to the boundary.
                 */
                var isAcceptedCoordinateFurther = false
                var isStackCoordinateFurther = false

                for (coordinate in lineCoordinates) {
                    val point = geoFac.createPoint(coordinate)

                    // a coordinate is a valid splitting point whenever it is at most DISTANCE_THRESHOLD away from
                    // the boundary of the parent element
                    val coordinateQualifies = DistanceOp.isWithinDistance(point, jtsBaseBoundary, DISTANCE_THRESHOLD)

                    if (!coordinateQualifies) {
                        if (coordinates.isNotEmpty()) {
                            // only add a coordinate to the stack if we have already begun with the split line
                            coordinateStack.add(point)
                            isStackCoordinateFurther = true
                        }

                        continue
                    }

                    coordinates.addAll(coordinateStack)
                    isAcceptedCoordinateFurther = isAcceptedCoordinateFurther || isStackCoordinateFurther
                    coordinateStack.clear()

                    /*
                     * check whether the found coordinate is an exact connection point towards the base element's
                     * boundary.
                     *
                     * If yes, we only have to add this point.
                     *
                     * If no, we have to add:
                     * * The current point (if it would not be the first "relevant" coordinate)
                     * * Then the nearest point that is directly on the boundary
                     * * Then again the current point on the line (if it is not the last point on the line to consider.
                     *
                     * Basically, create a shortest "bridge" between the line string and the base element boundary.
                     * Don't add the current point before the nearest point if the relevant line has not yet started
                     * and the current point is the first one.
                     *
                     * Also, add the current point to the stack again so that, if we find more relevant points, the
                     * "direct way back" is also added to the line so it does not get crooked
                     */
                    val isExact = jtsBaseBoundary.contains(point)

                    if (coordinates.isNotEmpty() || isExact) {
                        coordinates.add(point)
                    }


                    if (isExact) {
                        // if the point is exactly on the line, nothing to do here
                        continue
                    }

                    // else insert the nearest point of the boundary as an "extension"
                    val points = DistanceOp.nearestPoints(jtsBaseBoundary, point)

                    coordinates.add(geoFac.createPoint(points.first()))

                    // if this is not the last point on the line, also add the "reverse" way.
                    // => add it to the stack so it gets inserted if we find another point.
                    coordinateStack.add(point)
                }

                if (coordinates.size >= 2) {
                    val resultingLineString = geoFac.createLineString(coordinates.map { it.coordinate }.toTypedArray())

                    if (!isAcceptedCoordinateFurther) {
                        // if none of the coordinates in the resultingLineString is further away from the baseshape boundary,
                        // we need to calculate the Haussdorf distance to see if the elements are really that close together

                        val distanceCalculator = DiscreteHausdorffDistance(resultingLineString, jtsBaseBoundary)

                        distanceCalculator.setDensifyFraction(HAUSSDORF_DENSIFY)

                        val distance = distanceCalculator.orientedDistance()

                        if (distance > DISTANCE_THRESHOLD) {
                            isAcceptedCoordinateFurther = true
                        }

                    }

                    if (isAcceptedCoordinateFurther) {
                        lineStringSet.add(resultingLineString)
                    }
                }
            }


            if (lineStringSet.isNotEmpty()) {
                ret[ref] = lineStringSet
            }
        }

        return ret
    }

    private fun emptyResult(): Result {
        return Result(emptyMap(), emptySet(), HashBiMap.create())
    }

    companion object {
        /**
         * Currently has two functions:
         * * Maximum distance a line point can have from a shape's boundary to be still considered a point in a splitting line
         * * Minimum distance a line must have from a shape's boundary for the line to be considered a splitting line
         *   (and not a rounding error in the splitting)
         */
        const val DISTANCE_THRESHOLD: Double = 5.0

        /**
         * Threshold under which compartments will be ignored (used to rule out rounding errors)
         */
        const val AREA_THRESHOLD: Double = 10.0

        /**
         * Densify factor (fractional) for the Haussdorf line distance calculation.
         *
         * See also [DiscreteHausdorffDistance.setDensifyFraction]
         */
        const val HAUSSDORF_DENSIFY: Double = 0.5

        /**
         * As the JTS OverlaySnapRounded function used in this service is not exact and seems to have problems with
         * too long line segments in a shape, densify all base shapes so that no points on its outline are more than
         * [DENSIFY_DISTANCE] apart.
         * Not to be confused with [HAUSSDORF_DENSIFY], which is fractional and used for the calculation of the distance
         * between a shape border and its line.
         */
        const val DENSIFY_DISTANCE: Double = 5.0

        const val OVERLAY_SNAP_PRECISION = 10.0


    }

    /**
     * DTO to keep the resulting Geometries and the line-shaped [GraphicObject]s that are resulting from splitting
     * a Shape.
     */
    data class Result(
            /**
             * Sub-geometry
             */
            val geometries: Map<Pair<Int, Int>, ULTableValue>,
            val usedLines: Set<ElementReference<GraphicObject<*>>>,
            val indexes: BiMap<CompartmentIndex, Geometry>
    ) {
        sealed class ULTableValue {
            /**
             *
             */
            data class UniqueGeometry(val geometry: Geometry) : ULTableValue()

            data class BottomRightIdentified(val brIndexMap: Map<Pair<Int, Int>, BRTableValue>) : ULTableValue()
        }

        sealed class BRTableValue {
            data class UniqueGeometry(val geometry: Geometry) : BRTableValue()
            data class InteriorPointIdentified(val ipMap: Map<Pair<Double, Double>, Geometry>) : BRTableValue()
        }
    }


    private class ResultBuilder {
        private val leftXOrder = sortedSetOf(comparator)
        private val rightXOrder = sortedSetOf(comparator)
        private val topYOrder = sortedSetOf(comparator)
        private val bottomYOrder = sortedSetOf(comparator)

        private val geometriesWithBoundaries = mutableMapOf<Geometry, Rectangle>()

        fun add(geometry: Geometry, boundary: Rectangle) {
            this.geometriesWithBoundaries[geometry] = boundary

            this.leftXOrder.add(boundary.x)
            this.topYOrder.add(boundary.y)
            this.rightXOrder.add(boundary.x + boundary.w)
            this.bottomYOrder.add(boundary.y + boundary.h)
        }

        fun build(usedLines: Set<ElementReference<GraphicObject<*>>>): Result {
            val topLeftMap = HashMultimap.create<Pair<Int, Int>, Geometry>()

            for ((geometry, boundary) in geometriesWithBoundaries) {
                val topLeft = Pair(
                        leftXOrder.indexOf(boundary.x),
                        topYOrder.indexOf(boundary.y)
                )

                topLeftMap.put(topLeft, geometry)
            }

            val geometries = mutableMapOf<Pair<Int, Int>, Result.ULTableValue>()
            val indexes = HashBiMap.create<CompartmentIndex, Geometry>()

            for (upperLeftIndex in topLeftMap.keys()) {
                val geomSet = topLeftMap[upperLeftIndex]

                if (geomSet.size == 1) {
                    val geometry = geomSet.first()

                    geometries[upperLeftIndex] = Result.ULTableValue.UniqueGeometry(geometry)
                    indexes[CompartmentIndex(upperLeftIndex)] = geometry
                }

                if (geomSet.size > 1) {
                    // We have a collision!
                    // If we have more than one geometry with the same upper left boundary, check the bottom right
                    val bottomRightMap = HashMultimap.create<Pair<Int, Int>, Geometry>()

                    for (geometry in geomSet) {
                        val boundary = geometriesWithBoundaries.getValue(geometry)

                        val bottomRight = Pair(
                                rightXOrder.indexOf(boundary.x + boundary.w),
                                bottomYOrder.indexOf(boundary.y + boundary.h)
                        )

                        bottomRightMap.put(bottomRight, geometry)
                    }

                    val bottomRightIndexMap = mutableMapOf<Pair<Int, Int>, Result.BRTableValue>()

                    for (bottomRightIndex in bottomRightMap.keys()) {
                        val brGeomSet = bottomRightMap.get(bottomRightIndex)

                        if (brGeomSet.size == 1) {
                            val geometry = brGeomSet.first()

                            bottomRightIndexMap[bottomRightIndex] = Result.BRTableValue.UniqueGeometry(geometry)
                            indexes[CompartmentIndex(upperLeftIndex, bottomRightIndex)] = geometry
                        }

                        if (brGeomSet.size > 1) {
                            // Bottom right corner is identical, too!
                            // As a last resort, use an arbitrary interior point of each compartment.
                            // This will probably result in frequent Compartment Element changes

                            val interiorPointMap = mutableMapOf<Pair<Double, Double>, Geometry>()

                            for (identicalBBoxGeometry in brGeomSet) {
                                var geometry = identicalBBoxGeometry

                                for (other in brGeomSet) {
                                    if (identicalBBoxGeometry != other) {
                                        continue
                                    }
                                    geometry = geometry.difference(other)
                                }

                                val point = geometry.interiorPoint

                                val pointPair = Pair(point.x, point.y)

                                interiorPointMap[pointPair] = identicalBBoxGeometry
                                indexes[CompartmentIndex(upperLeftIndex, bottomRightIndex, pointPair)] = identicalBBoxGeometry
                            }

                            bottomRightIndexMap[bottomRightIndex] = Result.BRTableValue.InteriorPointIdentified(
                                    interiorPointMap
                            )
                        }
                    }

                    geometries[upperLeftIndex] = Result.ULTableValue.BottomRightIdentified(bottomRightIndexMap)
                }
            }

            return Result(geometries, usedLines, indexes)
        }

        companion object {
            val comparator = Comparator<Double> { v1, v2 ->
                return@Comparator v1.compareTo(v2)
            }
        }
    }
}
