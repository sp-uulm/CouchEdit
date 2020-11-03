package de.uulm.se.couchedit.client.interaction.creationtool

import com.google.inject.Inject
import de.uulm.se.couchedit.client.interaction.input.CanvasMouseEvent
import de.uulm.se.couchedit.client.interaction.pattern.InteractionPatternCallbacks
import de.uulm.se.couchedit.client.interaction.pattern.SelectMultiPoint
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.StraightSegmentLine
import de.uulm.se.couchedit.processing.common.services.datastore.IdGenerator

/**
 * [CreationTool] that can be used to generate a line (a generic one, i.e. without any associated abstract syntax
 * hints / information). This prompts the user to select a set of [Point]s via the [SelectMultiPoint] interaction, and
 * after the completion of this interaction creates a [StraightSegmentLine] [PrimitiveGraphicObject] which is then passed
 * to the [callbacks] in order for it to be inserted to the application data model.
 *
 * @param [idGenerator] Used for generating the ID of newly inserted [PrimitiveGraphicObject]
 */
class LineCreationTool @Inject constructor(private val idGenerator: IdGenerator) : CreationTool(), InteractionPatternCallbacks<List<Point>> {
    override fun onClick(e: CanvasMouseEvent) {
        if (!isActive) {
            val startPoint = e.coordinates.copy()

            this.currentInteractionPattern = SelectMultiPoint(startPoint, this)

            this.activate()
        } else {
            super.onClick(e)
        }
    }

    override fun onResult(value: List<Point>) {
        this.currentInteractionPattern = null

        val resultShape = StraightSegmentLine(value.toList())

        val result = PrimitiveGraphicObject(idGenerator.generate(PrimitiveGraphicObject::class.java), resultShape)

        this.finishInteraction(setOf(result))
    }

    override fun onAbort() {
        this.abortInteraction()
    }
}
