package de.uulm.se.couchedit.statecharts.testdata

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineEndPointStyle
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryProvider
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator.StateRepresentation
import de.uulm.se.couchedit.util.extensions.ref
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.prep.PreparedGeometryFactory
import org.locationtech.jts.linearref.LengthIndexedLine
import org.locationtech.jts.operation.distance.DistanceOp

/**
 * Test data generator which can generate concrete syntax representations of transitions between states (line + attributes),
 * the latter as created by the [StateGridGenerator].
 *
 * @param connectionEndDistance The distance that lines' endpoints will have from the state representations
 * @param lineZValue The [GraphicObject.z] value that all generated lines will get
 */
class StateTransitionGenerator(private val connectionEndDistance: Double, private val lineZValue: List<Int>) {
    private val shapeJTSGeometryProvider = JTSGeometryProvider(PreparedGeometryFactory())
    private val geometryFactory = GeometryFactory()

    /**
     * Creates a concrete syntax representation (line + attributes + attribute association) of a transition between
     * the states represented by the [sourceStateRepresentation] and the [targetStateRepresentation].
     */
    fun createConnectionBetween(
            sourceStateRepresentation: StateRepresentation,
            targetStateRepresentation: StateRepresentation
    ): TransitionRepresentation {
        val (line, borderPointSource, borderPointTarget) = getTransitionLineAndBorderPoints(
                sourceStateRepresentation,
                targetStateRepresentation
        )

        val lineId = sourceStateRepresentation.stateId + "<to>" + targetStateRepresentation.stateId

        val goLine = PrimitiveGraphicObject(lineId, line)
        goLine.z = lineZValue.toMutableList()

        val attributes = LineAttributes("${lineId}_attr").apply {
            this[GraphicAttributeKeys.LINE_END_STYLE] = LineEndPointStyle(LineEndPointStyle.Option.SOLIDARROW)
        }

        val attributesFor = AttributesFor(attributes.ref(), goLine.ref())

        return TransitionRepresentation(mapOf(
                ElementRole.ATTRIBUTES to attributes,
                ElementRole.ATTRIBUTE_FOR_RELATION to attributesFor,
                ElementRole.LINE to goLine
        ), borderPointSource, borderPointTarget)
    }

    /**
     * Modifies the line in [oldRepr] so that it now connects [sourceStateRepresentation] to [targetStateRepresentation].
     * This keeps the old IDs from the [oldRepr], which may be confusing but enables to simulate "move" operations.
     */
    fun createConnectionBetweenKeepingOldId(
            sourceStateRepresentation: StateRepresentation,
            targetStateRepresentation: StateRepresentation,
            oldRepr: TransitionRepresentation
    ): TransitionRepresentation {
        val (line, borderPointSource, borderPointTarget) = getTransitionLineAndBorderPoints(
                sourceStateRepresentation,
                targetStateRepresentation
        )

        val newLineElement = oldRepr.line.copy() as PrimitiveGraphicObject<*>

        newLineElement.setContentFrom(line)

        return TransitionRepresentation(mapOf(
                ElementRole.ATTRIBUTES to oldRepr.getValue(ElementRole.ATTRIBUTES),
                ElementRole.ATTRIBUTE_FOR_RELATION to oldRepr.getValue(ElementRole.ATTRIBUTE_FOR_RELATION),
                ElementRole.LINE to newLineElement
        ), borderPointSource, borderPointTarget)
    }

    /**
     * Generates a [Table] of [TransitionRepresentation], where the value of each cell connects the State Representation with [StateRepresentation.stateId] == rowKey to the State Representation with
     * [StateRepresentation.stateId] == columnKey. The connections to be made are those from the (source, target) pairs in [list]
     */
    fun createTransitionTableBetween(list: List<Pair<StateRepresentation, StateRepresentation>>): Table<String, String, TransitionRepresentation> {
        val ret = HashBasedTable.create<String, String, TransitionRepresentation>()

        for ((source, target) in list) {
            val transitionRepresentation = createConnectionBetween(source, target)

            ret.put(source.stateId, target.stateId, transitionRepresentation)
        }

        return ret
    }

    /**
     * Gets a [StraightSegmentLine] connecting [sourceStateRepresentation] to [targetStateRepresentation], and
     */
    private fun getTransitionLineAndBorderPoints(
            sourceStateRepresentation: StateRepresentation,
            targetStateRepresentation: StateRepresentation
    ): Triple<StraightSegmentLine, Point, Point> {
        val borderPoints = getConnectionPointsOnBorder(sourceStateRepresentation, targetStateRepresentation)

        val distancedPoints = getConnectionPointsWithDistance(borderPoints)

        val lineId = sourceStateRepresentation.stateId + "<to>" + targetStateRepresentation.stateId

        val line = StraightSegmentLine(distancedPoints.toList())

        return Triple(line, borderPoints.first, borderPoints.second)
    }

    /**
     * Gets the points for the connection if it was created directly on the state's rounded rectangle border.
     */
    private fun getConnectionPointsOnBorder(
            sourceStateRepresentation: StateRepresentation,
            targetStateRepresentation: StateRepresentation
    ): Pair<Point, Point> {
        val sourceRoundedRectangle = sourceStateRepresentation.outerStateRectangle.shape as RoundedRectangle
        val targetRoundedRectangle = targetStateRepresentation.outerStateRectangle.shape as RoundedRectangle

        val sourceGeometry = shapeJTSGeometryProvider.toGeometry(sourceRoundedRectangle).geometry
        val targetGeometry = shapeJTSGeometryProvider.toGeometry(targetRoundedRectangle).geometry

        val distanceOp = DistanceOp(sourceGeometry, targetGeometry)

        val nearestCoordinates = distanceOp.nearestPoints()

        return Pair(coordinateToPoint(nearestCoordinates[0]), coordinateToPoint(nearestCoordinates[1]))
    }

    /**
     * From previously calculated points on the border of state shapes, calculates points that are
     * [connectionEndDistance] away from the border, closer to the corresponding other shape.
     */
    private fun getConnectionPointsWithDistance(borderPoints: Pair<Point, Point>): Pair<Point, Point> {
        val line = geometryFactory.createLineString(arrayOf(
                pointToCoordinate(borderPoints.first),
                pointToCoordinate(borderPoints.second)
        ))

        val lengthIndexedLine = LengthIndexedLine(line)

        val coordinateFrom = lengthIndexedLine.extractPoint(connectionEndDistance)
        val coordinateTo = lengthIndexedLine.extractPoint(-connectionEndDistance)

        return Pair(Point(coordinateFrom.x, coordinateFrom.y), Point(coordinateTo.x, coordinateTo.y))
    }

    /**
     * Container object for the GraphicObjects participating in the concrete syntax representation of a state transition.
     *
     * @param map Map from the role of each Element in the connection-representing graphic to the Element
     * @param fromShapeBorderPoint Point on the border of the state shape where the transition originates from, closest
     *                             to the "to" shape's border
     * @param toShapeBorderPoint Point on the border of the state shape where the connection goes to, closest to the
     *                           "from" shape's border
     */
    class TransitionRepresentation(
            private val map: Map<ElementRole, Element>,
            val fromShapeBorderPoint: Point,
            val toShapeBorderPoint: Point
    ) : Map<ElementRole, Element> by map {
        val line = map[ElementRole.LINE] as GraphicObject<*>
    }

    enum class ElementRole {
        ATTRIBUTES,
        ATTRIBUTE_FOR_RELATION,
        LINE
    }

    companion object {
        fun coordinateToPoint(coordinate: Coordinate) = Point(coordinate.x, coordinate.y)
        fun pointToCoordinate(point: Point) = Coordinate(point.x, point.y)
    }
}
