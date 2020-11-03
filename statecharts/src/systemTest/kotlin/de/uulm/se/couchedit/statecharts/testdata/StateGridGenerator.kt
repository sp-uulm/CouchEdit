package de.uulm.se.couchedit.statecharts.testdata

import com.google.common.collect.Table
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.statecharts.testdata.StateRepresentationGenerator.GraphicObjectRole.*
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator

class StateGridGenerator(
        private val gridAreasGenerator: GridAreasGenerator,
        private val stateRepresentationGenerator: StateRepresentationGenerator
) {
    /**
     * Generates a grid of rounded rectangles, each containing a label with a sequential number.
     *
     * @param gridSizeX The number of States to create in horizontal direction
     * @param gridSizeY The number of States to create in vertical direction
     * @param totalWidth Total width of the statechart drawing
     * @param totalHeight Total height of the statechart drawing
     * @param objectDistance Distance between two state symbols in the drawing
     * @param roundedEdgeSize The horizontal and vertical size that the rounded corners of the rectangles should have
     *
     * @return Grid of state concrete syntax representations
     */
    fun generateGridOfLabeledStates(
            gridSizeX: Int,
            gridSizeY: Int,
            xOffset: Double = 0.0,
            yOffset: Double = 0.0,
            idPrefix: String = ""
    ): Table<Int, Int, StateRepresentationGenerator.StateRepresentation> {
        var idCount = 0

        return gridAreasGenerator.generateGrid(
                gridSizeX,
                gridSizeY,
                xOffset,
                yOffset
        ) { area ->
            val commonId = "${idPrefix}_$idCount"

            idCount++

            return@generateGrid stateRepresentationGenerator.getStateRepresentationFrom(area, commonId)
        }
    }

    /**
     * Out of the given [table], modifies [number] state representations so that they are now located to the right
     * of the previously right border of the grid represented by [table].
     *
     * @param table The grid of [StateRepresentation]s of which some should be moved to the current right border
     * @param number The number of [StateRepresentation]s which should be moved to the right of the grid; These will be
     *               selected out of the middle of the grid and then uniformly moved so that they are [objectDistance]
     *               away from the previous right border of the grid.
     *
     * @return Set of [StateRepresentation] objects of which the contents were modified.
     */
    fun moveStatesToRight(table: Table<Int, Int, StateRepresentationGenerator.StateRepresentation>, number: Int): Set<StateRepresentationGenerator.StateRepresentation> {
        return gridAreasGenerator.moveElementsToRightOfGrid(table, number, {
            val outerRect = it.getValue(OUTER_STATE_RECTANGLE).shape as Rectangular

            return@moveElementsToRightOfGrid with(outerRect) {
                GridAreasGenerator.Area(x, y, w, h)
            }
        }, { element, area ->
            val outerRect = element.outerStateRectangle.shape as Rectangular
            val labelRect = element.label?.shape as? Rectangular

            setRectangularFromArea(outerRect, area)
            labelRect?.let { setRectangularFromArea(it, stateRepresentationGenerator.generateLabelArea(area)) }

            return@moveElementsToRightOfGrid element
        })
    }

    private fun setRectangularFromArea(rectangular: Rectangular, area: GridAreasGenerator.Area) {
        with(rectangular) {
            x = area.x
            y = area.y
            w = area.w
            h = area.h
        }
    }
}
