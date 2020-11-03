package de.uulm.se.couchedit.processing.spatial.services.geometric

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape

@Singleton
class JTSShapeBoundsCalculator @Inject constructor(
        private val jtsGeometryProvider: JTSGeometryProvider,
        private val jtsGeometryBoundsCalculator: JTSGeometryBoundsCalculator
) : ShapeBoundsCalculator {
    override fun getBoundingBox(shape: Shape, objectId: String?): Rectangle {
        val geometry = jtsGeometryProvider.toGeometry(shape, objectId)

        return jtsGeometryBoundsCalculator.getBoundingRectangle(geometry.geometry)
    }

}
