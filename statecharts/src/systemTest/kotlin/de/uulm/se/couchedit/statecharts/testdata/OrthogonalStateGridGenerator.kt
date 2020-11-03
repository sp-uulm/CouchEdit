package de.uulm.se.couchedit.statecharts.testdata

import com.google.common.collect.Table
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.graphic.attributes.GraphicAttributeKeys
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import de.uulm.se.couchedit.statecharts.testdata.OrthogonalStateLineRepresentation.ElementRole.*
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator
import de.uulm.se.couchedit.util.extensions.ref

/**
 * Test Data Generation helper which, from an [GridAreasGenerator.Area] occupied by a State Representation, generates
 * a grid of horizontal and vertical lines so that a grid of orthogonal states results.
 */
class OrthogonalStateGridGenerator(private val gridSizeX: Int, private val gridSizeY: Int, private val lineMargin: Double) {

    /**
     * Generates a Grid of [gridSizeX] x [gridSizeY] Compartments, i.e. [gridSizeX] - 1 vertical and [gridSizeY] - 1
     * horizontal lines.
     * All of the lines are generated as [OrthogonalStateLineRepresentation]s, i.e. they also have Attributes
     * associated to them that make them dashed.
     *
     * @param outerArea The Area in which the grid lines will be evenly distributed
     *
     * @return Result containing the grid lines and the approximate Compartment areas resulting from them.
     */
    fun generateCompartmentGrid(outerArea: GridAreasGenerator.Area): CompartmentGridResult {
        val gridAreasGenerator = GridAreasGenerator(outerArea.h / gridSizeY, outerArea.w / gridSizeX, 0.0)

        val outerAreaWithMargin = GridAreasGenerator.reduceAreaByMargin(outerArea, lineMargin)

        // simply output the areas. we will map them in the following steps.
        val grid = gridAreasGenerator.generateGrid(gridSizeX, gridSizeY) { it }

        val verticalLines = grid.columnMap().toSortedMap().mapNotNull { (xPos, column) ->
            if (xPos == gridSizeX - 1) {
                return@mapNotNull null
            }

            val rightX = column.values.first().rightX

            val verticalLineShape = StraightSegmentLine(listOf(
                    Point(rightX, outerAreaWithMargin.y),
                    Point(rightX, outerAreaWithMargin.bottomY)
            ))

            generateLineRepresentation(PrimitiveGraphicObject("vertical_$xPos", verticalLineShape))
        }

        val horizontalLines = grid.rowMap().toSortedMap().mapNotNull { (yPos, row) ->
            if (yPos == gridSizeY - 1) {
                return@mapNotNull null
            }

            val bottomY = row.values.first().bottomY

            val verticalLineShape = StraightSegmentLine(listOf(
                    Point(outerAreaWithMargin.x, bottomY),
                    Point(outerAreaWithMargin.rightX, bottomY)
            ))

            generateLineRepresentation(PrimitiveGraphicObject("horizontal_$yPos", verticalLineShape))
        }

        return CompartmentGridResult(grid, verticalLines, horizontalLines)
    }

    private fun generateLineRepresentation(graphicObject: PrimitiveGraphicObject<Line>): OrthogonalStateLineRepresentation {
        val attributes = LineAttributes("${graphicObject.id}_attr")
        attributes[GraphicAttributeKeys.LINE_STYLE] = LineStyle(LineStyle.Option.DASHED)

        val attributesFor = AttributesFor(attributes.ref(), graphicObject.ref())

        val map = mapOf(
                LINE to graphicObject,
                ATTRIBUTES to attributes,
                ATTRIBUTES_FOR to attributesFor
        )

        return OrthogonalStateLineRepresentation(map)
    }

    data class CompartmentGridResult(
            /**
             * (Approximate) Table of compartment areas that exist in this grid once processed.
             * As always, X and Y are reversed because of the parameter names in Guava.
             *
             * This means, the cell `(y, x)` contains the compartment between
             * * The (y-1)th line at the top (or the top border of the outer area if `y == 0`)
             * * The (y)th line at the bottom (or the bottom border of the outer area if `y == rowCount - 1`)
             * * The (x-1)th line to the left (or the left border of the outer area if `x == 0`)
             * * The (x)th line to the right (or the right border of the outer area if `x == columnCount - 1`)
             */
            val grid: Table<Int, Int, GridAreasGenerator.Area>,
            /**
             * The lines that segment the given outer area vertically.
             *
             * Their count is equal to the [grid]'s columnCount - 1.
             */
            val verticalLines: List<OrthogonalStateLineRepresentation>,
            /**
             * The lines that segment the given outer area horizontally.
             *
             * Their count is equal to the [grid]'s rowCount - 1
             */
            val horizontalLines: List<OrthogonalStateLineRepresentation>
    ) {
        val allLines = verticalLines.union(horizontalLines)
    }

    private val GridAreasGenerator.Area.rightX
        get() = this.x + this.w


    private val GridAreasGenerator.Area.bottomY
        get() = this.y + this.h
}

