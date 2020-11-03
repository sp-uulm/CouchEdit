package de.uulm.se.couchedit.processing.compartment.services

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.compartment.CompartmentIndex
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory

@Singleton
class SplitResultInterpreter @Inject constructor(private val geometryFactory: GeometryFactory) {
    fun getGeometryByIndex(result: CompartmentGeometryGenerator.Result, index: CompartmentIndex): Geometry? {
        when (val ulContainer = result.geometries[index.indexUL] ?: return null) {
            is CompartmentGeometryGenerator.Result.ULTableValue.UniqueGeometry -> {
                if (index.indexBR != null) {
                    return null
                }

                return ulContainer.geometry
            }
            is CompartmentGeometryGenerator.Result.ULTableValue.BottomRightIdentified -> {
                when (val brContainer = ulContainer.brIndexMap[index.indexBR] ?: return null) {
                    is CompartmentGeometryGenerator.Result.BRTableValue.UniqueGeometry -> {
                        if (index.interiorPoint != null) {
                            return null
                        }

                        return brContainer.geometry
                    }
                    is CompartmentGeometryGenerator.Result.BRTableValue.InteriorPointIdentified -> {
                        if (index.interiorPoint == null) {
                            return null
                        }

                        val jtsInteriorPoint = geometryFactory.createPoint(Coordinate(
                                index.interiorPoint.first,
                                index.interiorPoint.second
                        ))

                        val candidateGeometries = brContainer.ipMap.values.filter {
                            it.contains(jtsInteriorPoint)
                        }

                        if (candidateGeometries.size == 1) {
                            return candidateGeometries.first()
                        }

                        return null
                    }
                }
            }
        }
    }
}
