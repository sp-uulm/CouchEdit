package de.uulm.se.couchedit.processing.graphic.services

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.hotspots.LineFractionHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.processing.compartment.services.CompartmentGeometryGenerator
import de.uulm.se.couchedit.processing.hotspot.services.HotSpotProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeProvider
import de.uulm.se.couchedit.util.collection.TTLMap
import de.uulm.se.couchedit.util.extensions.getTyped
import org.locationtech.jts.linearref.LengthIndexedLine
import java.util.*

@Singleton
class LineFractionHotSpotProvider @Inject constructor(
        private val shapeExtractor: ShapeExtractor,
        private val jtsGeometryProvider: JTSGeometryProvider
): HotSpotProvider<Point, LineFractionHotSpotDefinition> {
    private val pointCache = TTLMap<Int, Point>()

    private val elementCacheKeys = mutableMapOf<ElementReference<*>, Int>()

    override fun generateShape(
            hRef: ElementReference<LineFractionHotSpotDefinition>,
            elements: Map<ElementReference<*>, Element>
    ): Point? {
        val hsd = elements.getTyped(hRef) ?: throw IllegalArgumentException("Elements map must contain $hRef")

        val lineShape = shapeExtractor.extractShape(hsd.a, elements) ?: throw IllegalArgumentException(
                "${hsd.a} did not prevent a Line!"
        )

        val cacheKey = generateCacheHash(lineShape, hsd.offset)

        val oldCacheKey = elementCacheKeys[hRef]
        if(oldCacheKey != null && oldCacheKey != cacheKey) {
            pointCache.remove(oldCacheKey)
        }

        elementCacheKeys[hRef] = cacheKey

        pointCache[cacheKey]?.let { return it }

        val geometry = jtsGeometryProvider.toGeometry(lineShape, hsd.a.id)

        val length = geometry.geometry.length

        val indexedLine = LengthIndexedLine(geometry.geometry)

        val pointGeometry = indexedLine.extractPoint(hsd.offset * length)

        val point = Point(pointGeometry.x, pointGeometry.y)

        pointCache.put(cacheKey,point)

        return point
    }

    private fun generateCacheHash(line: Line, fraction: Double): Int {
        return Objects.hash(line, fraction)
    }
}
