package de.uulm.se.couchedit.statecharts.testdata

import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
import de.uulm.se.couchedit.testsuiteutils.testdata.grid.GridAreasGenerator

class StateRepresentationGenerator(private val roundedEdgeSize: Double) {
    /**
     * Generates a State concrete syntax representation (Rounded rectangle + Label) which is contained in the given
     * [area].
     *
     * @param area The area which the State representation should be contained in
     * @param commonId The "common" ID for the group of GraphicObjects to be created.
     *                 The ID is used as the [StateRepresentation.stateId] of the StateElement, as the label's text
     *                 (with the Prefix "S") and as the ID of both the rounded rectangle, suffixed as "_rect" and
     *                 "_label", respectively.
     * @param labelPosition The position that the label should have within its state's shape
     *
     * @return StateRepresentation Element which contains the
     */
    fun getStateRepresentationFrom(
            area: GridAreasGenerator.Area,
            commonId: String,
            labelPosition: LabelPosition = LabelPosition.Center
    ): StateRepresentation {
        val ret = mutableMapOf<GraphicObjectRole, GraphicObject<*>>()

        val outerRectangular = with(area) {
            RoundedRectangle(x, y, w, h, roundedEdgeSize)
        }
        val goOuterRectangular = PrimitiveGraphicObject("${commonId}_rect", outerRectangular)

        ret[GraphicObjectRole.OUTER_STATE_RECTANGLE] = goOuterRectangular

        val labelArea = if (labelPosition != LabelPosition.None) {
            val newLabelArea = generateLabelArea(area, labelPosition)

            val label = with(newLabelArea) {
                de.uulm.se.couchedit.model.graphic.shapes.Label(x, y, w, h, "S$commonId")
            }
            val goLabel = PrimitiveGraphicObject("${commonId}_label", label)
            ret[GraphicObjectRole.LABEL] = goLabel

            newLabelArea
        } else null

        val innerArea = when (labelPosition) {
            LabelPosition.None -> area
            LabelPosition.Center -> null
            is LabelPosition.Top -> {
                GridAreasGenerator.Area(area.x, area.y + labelArea!!.h, area.w, area.h - labelArea.h)
            }
        }

        return StateRepresentation(commonId, ret, labelPosition, area, innerArea)
    }

    /**
     * Gets a new [StateRepresentation] which is in the given [area] and matches the [oldRepr] in its properties
     *
     * @param area The area where the new [StateRepresentation] fits in
     * @param oldRepr The [StateRepresentation] where the properties should be taken from
     *
     * @return New [StateRepresentation] matching the properties of [oldRepr].
     */
    fun getStateRepresentationFrom(
            area: GridAreasGenerator.Area,
            oldRepr: StateRepresentation
    ): StateRepresentation {
        return getStateRepresentationFrom(area, oldRepr.stateId, oldRepr.labelPosition)
    }

    /**
     * Generates the area allocated to a StateRepresentation's label based on the RoundedRectangle's allocated area
     * [outerArea].
     */
    fun generateLabelArea(outerArea: GridAreasGenerator.Area, labelPosition: LabelPosition = LabelPosition.Center): GridAreasGenerator.Area {
        require(labelPosition != LabelPosition.None) { "Cannot generate a label Area for the \"None\" position" }

        require(!(labelPosition is LabelPosition.Top && labelPosition.height > outerArea.h)) {
            "Label with height ${(labelPosition as LabelPosition.Top).height} does not fit into outer area with height ${outerArea.h}"
        }

        return with(outerArea) {
            val lX = x + roundedEdgeSize
            val lY = if (labelPosition == LabelPosition.Center) y + roundedEdgeSize else y
            val lW = w - 2 * roundedEdgeSize
            val lH = if (labelPosition is LabelPosition.Top) labelPosition.height else h - 2 * roundedEdgeSize

            GridAreasGenerator.Area(lX, lY, lW, lH)
        }
    }

    /**
     * Container object for the GraphicObjects contained in the concrete syntax representation of a state.
     *
     * @param stateId Common ID given to the state representation, can be used for referencing within test cases
     * @param map Map of the GraphicObject roles contained within a state representation to the GraphicObject fulfilling
     *            that role.
     */
    class StateRepresentation(
            val stateId: String,
            private val map: Map<GraphicObjectRole, GraphicObject<*>>,
            val labelPosition: LabelPosition,
            val outerStateArea: GridAreasGenerator.Area,
            /**
             * The (approximate, without regard for rounded edges etc) Area that can be used for inserting child states
             * (i.e. that is not occupied with the label), or null if no such Area is available
             */
            val stateInteriorArea: GridAreasGenerator.Area?
    ) : Map<GraphicObjectRole, GraphicObject<*>> by map {
        val outerStateRectangle = map.getValue(GraphicObjectRole.OUTER_STATE_RECTANGLE)
        val label = map[GraphicObjectRole.LABEL]
    }

    enum class GraphicObjectRole {
        OUTER_STATE_RECTANGLE,
        LABEL
    }

    sealed class LabelPosition {
        object None : LabelPosition()
        object Center : LabelPosition()
        class Top(val height: Double) : LabelPosition()
    }
}
