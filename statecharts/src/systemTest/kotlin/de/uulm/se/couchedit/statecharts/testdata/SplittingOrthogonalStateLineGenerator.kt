package de.uulm.se.couchedit.statecharts.testdata

import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref

/**
 * Test concrete syntax generation helper which is able to create Orthogonal State representations (insert dashed
 * lines into given [GridAreasGenerator.Area]s
 *
 * @param lineMargin The margin that is left in the parent area to each side of the splitting line.
 */
class SplittingOrthogonalStateLineGenerator(private val lineMargin: Double) {
    /**
     * Gets a line that splits the given [area] in two either horizontally or vertically, based on the given
     * [orientation].
     *
     * @param area The Area to split in half.
     *
     * @return [SplitResult] that contains the selected orientation, the Line GraphicObject which can be used to split
     *         the given [area] into two halves and finally the areas that will result in the splitting.
     */
    fun split(lineId: String, area: GridAreasGenerator.Area, orientation: OrthogonalStateLineRepresentation.LineOrientation): SplitResult {
        return when (orientation) {
            OrthogonalStateLineRepresentation.LineOrientation.HORIZONTAL -> getHorizontalSplittingLine(lineId, area)
            OrthogonalStateLineRepresentation.LineOrientation.VERTICAL -> getVerticalSplittingLine(lineId, area)
        }
    }

    private fun getHorizontalSplittingLine(lineId: String, area: GridAreasGenerator.Area): SplitResult {
        val splitAreaHeight = 0.5 * area.h

        val (linePoint1, linePoint2) = with(area) {
            val x1 = x + lineMargin
            val x2 = x + w - lineMargin

            val y = y + splitAreaHeight

            return@with Point(x1, y) to Point(x2, y)
        }

        val (upperArea, lowerArea) = with(area) {
            val y1 = y
            val y2 = y + splitAreaHeight

            return@with (GridAreasGenerator.Area(x, y1, w, splitAreaHeight)
                    to GridAreasGenerator.Area(x, y2, w, splitAreaHeight))
        }

        val line = getLineGraphicObject(lineId, linePoint1, linePoint2)

        return getSplitResult(
                OrthogonalStateLineRepresentation.LineOrientation.HORIZONTAL,
                line,
                Pair(upperArea, lowerArea)
        )
    }

    private fun getVerticalSplittingLine(lineId: String, area: GridAreasGenerator.Area): SplitResult {
        val splitAreaWidth = 0.5 * area.w

        val (linePoint1, linePoint2) = with(area) {
            val x = x + splitAreaWidth

            val y1 = y + lineMargin
            val y2 = y + h - lineMargin

            return@with Point(x, y1) to Point(x, y2)
        }

        val (leftArea, rightArea) = with(area) {
            val x1 = x
            val x2 = x + splitAreaWidth

            return@with (GridAreasGenerator.Area(x1, y, splitAreaWidth, h)
                    to GridAreasGenerator.Area(x2, y, splitAreaWidth, h))
        }

        val line = getLineGraphicObject(lineId, linePoint1, linePoint2)

        return getSplitResult(
                OrthogonalStateLineRepresentation.LineOrientation.VERTICAL,
                line,
                Pair(leftArea, rightArea)
        )
    }

    private fun getSplitResult(
            orientation: OrthogonalStateLineRepresentation.LineOrientation,
            line: GraphicObject<Line>,
            areas: Pair<GridAreasGenerator.Area, GridAreasGenerator.Area>
    ): SplitResult {
        val attributes = LineAttributes("${line.id}_attr")

        attributes[GraphicAttributeKeys.LINE_STYLE] = LineStyle(LineStyle.Option.DASHED)

        val attributesFor = AttributesFor(attributes.ref(), line.ref())

        val map = mapOf(
                OrthogonalStateLineRepresentation.ElementRole.LINE to line,
                OrthogonalStateLineRepresentation.ElementRole.ATTRIBUTES to attributes,
                OrthogonalStateLineRepresentation.ElementRole.ATTRIBUTES_FOR to attributesFor
        )

        return SplitResult(orientation, OrthogonalStateLineRepresentation(map), areas)
    }

    private fun getLineGraphicObject(id: String, point1: Point, point2: Point): GraphicObject<Line> {
        val shape = StraightSegmentLine(listOf(point1, point2))

        return PrimitiveGraphicObject(id, shape)
    }

    class SplitResult(
            val orientation: OrthogonalStateLineRepresentation.LineOrientation,
            val lineRepresentation: OrthogonalStateLineRepresentation,
            val areas: Pair<GridAreasGenerator.Area, GridAreasGenerator.Area>
    ) : Map<OrthogonalStateLineRepresentation.ElementRole, Element> by lineRepresentation {
        val line = lineRepresentation.line
    }
}
