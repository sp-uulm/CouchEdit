package de.uulm.se.couchedit.processing.compartment.services

import com.google.common.collect.BiMap
import com.google.common.collect.Multimap
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentIndex
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.util.collection.TTLMap
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString

/**
 * Caches compartment calculation results so that expensive calculations don't need to be repeated every time.
 *
 * The results are stored based on the [Geometry]s participating and are then mapped back to the given [GraphicObject]s.
 * This prevents getting the wrong references of no longer existing Elements when Elements are later recreated with
 * the same geometry.
 */
class CompartmentCache {
    private val ttlMap = TTLMap<CacheKey, CacheContent>()

    /**
     * Stores the given [result] into this cache with the information of the given [lineGeometries].
     *
     * @param baseGeometry The geometry split up by the given [lines] giving the [result].
     * @param lineGeometries The geometries splitting up [baseGeometry] giving the [result].
     * @return [result]
     */
    fun storeAndReturn(
            baseGeometry: Geometry,
            lineGeometries: Map<ElementReference<GraphicObject<*>>, LineString>,
            result: CompartmentGeometryGenerator.Result
    ): CompartmentGeometryGenerator.Result {
        val key = this.generateCacheKeyFrom(baseGeometry, lineGeometries.values)

        val geometrySet = mutableSetOf<LineString>()

        for ((go, lineString) in lineGeometries) {
            if (go in result.usedLines) {
                geometrySet.add(lineString)
            }
        }

        ttlMap.put(key, CacheContent(result.geometries, result.indexes, geometrySet))

        return result
    }

    /**
     * Retrieves the [CompartmentGeometryGenerator.Result] stored for the given [baseGeometry] and the [Geometry]s making
     * up the keys of [lineGeometries], where the line geometries are replaced by the [ElementReference]s that the
     * [lineGeometries] maps to them.
     */
    fun retrieve(
            baseGeometry: Geometry,
            lineGeometries: Multimap<LineString, ElementReference<GraphicObject<*>>>
    ): CompartmentGeometryGenerator.Result? {
        val key = this.generateCacheKeyFrom(baseGeometry, lineGeometries.keySet())

        val cacheContent = ttlMap[key] ?: return null

        val elementReferences = cacheContent.usedLines.flatMap { lineGeometries.get(it) }.toSet()

        return CompartmentGeometryGenerator.Result(cacheContent.resultingGeometries, elementReferences, cacheContent.indexes)
    }

    /**
     * Generates an hashable cache key from the hashCodes of the given [baseGeometry] and [lineGeometries].
     */
    private fun generateCacheKeyFrom(baseGeometry: Geometry, lineGeometries: Collection<LineString>): CacheKey {
        return CacheKey(baseGeometry, lineGeometries.toSet())
    }

    private data class CacheKey(
            val baseGeometry: Geometry,
            val lineGeometries: Set<LineString>
    )

    private data class CacheContent(
            val resultingGeometries: Map<Pair<Int, Int>, CompartmentGeometryGenerator.Result.ULTableValue>,
            val indexes: BiMap<CompartmentIndex, Geometry>,
            val usedLines: Set<LineString>
    )
}
