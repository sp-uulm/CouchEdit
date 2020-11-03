package de.uulm.se.couchedit.processing.spatial.controller

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.spatial.relations.*
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.util.extensions.asGenericShape
import org.locationtech.jts.geom.prep.PreparedGeometry

@ProcessorScoped
class SpatialRelationChecker @Inject constructor(
        private val geometryProvider: JTSGeometryProvider,
        private val shapeExtractor: ShapeExtractor,
        private val caller: ServiceCaller
) {
    /**
     * Returns a list of [SpatialRelation]s that are valid between [fromRef] and [toRef].
     */
    fun getSpatialRelations(fromRef: ElementReference<ShapedElement<*>>, toRef: ElementReference<ShapedElement<*>>): List<SpatialRelation> {
        val fromShape = caller.call(fromRef.asGenericShape(), shapeExtractor::extractShape) ?: return listOf()
        val toShape = caller.call(toRef.asGenericShape(), shapeExtractor::extractShape) ?: return listOf()

        val fromGeometry = this.geometryProvider.toGeometry(fromShape)
        val toGeometry = this.geometryProvider.toGeometry(toShape)

        val ret = this.getIntersectionProperties(fromRef, toRef, fromGeometry, toGeometry).toMutableList()

        if (ret.isNotEmpty()) {
            return ret
        }

        /*
         * after 9IM has been evaluated, calculate in which spatial relation from and to stand if they are not
         * included / intersecting each other.
         */

        val fromBoundary = fromGeometry.geometry.envelopeInternal
        val toBoundary = toGeometry.geometry.envelopeInternal

        when {
            fromBoundary.minX > toBoundary.maxX -> ret.add(RightOfBoundary(fromRef, toRef))
            toBoundary.minX > fromBoundary.maxX -> ret.add(RightOfBoundary(toRef, fromRef))
            toBoundary.minX > fromBoundary.minX && toBoundary.maxX > fromBoundary.maxX -> ret.add(RightOf(toRef, fromRef))
            fromBoundary.minX > toBoundary.minX && fromBoundary.maxX > toBoundary.maxX -> ret.add(RightOf(fromRef, toRef))
        }

        when {
            fromBoundary.minY > toBoundary.maxY -> ret.add(BottomOfBoundary(fromRef, toRef))
            toBoundary.minY > fromBoundary.maxY -> ret.add(BottomOfBoundary(toRef, fromRef))
            toBoundary.minY > fromBoundary.minY && toBoundary.maxY > fromBoundary.maxY -> ret.add(BottomOf(toRef, fromRef))
            fromBoundary.minY > toBoundary.minY && fromBoundary.maxY > toBoundary.maxY -> ret.add(BottomOf(fromRef, toRef))
        }

        return ret.toList()
    }

    private fun getIntersectionProperties(
            fromRef: ElementReference<ShapedElement<*>>,
            toRef: ElementReference<ShapedElement<*>>,
            fromGeometry: PreparedGeometry,
            toGeometry: PreparedGeometry
    ): List<SpatialRelation> {
        if (fromGeometry.disjoint(toGeometry.geometry)) {
            return emptyList()
        }

        if (fromGeometry.covers(toGeometry.geometry)) {
            return listOf(Include(fromRef, toRef))
        }

        if (fromGeometry.coveredBy(toGeometry.geometry)) {
            return listOf(Include(toRef, fromRef))
        }

        return listOf(Intersect(fromRef, toRef))
    }
}
