package de.uulm.se.couchedit.processing.spatial.services.geometric

import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape

interface ShapeBoundsCalculator {
    fun getBoundingBox(shape: Shape, objectId: String? = null): Rectangle
}
