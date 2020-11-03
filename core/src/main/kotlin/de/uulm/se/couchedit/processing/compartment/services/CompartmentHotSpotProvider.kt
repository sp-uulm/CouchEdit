package de.uulm.se.couchedit.processing.compartment.services

import com.google.inject.Inject
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Polygon
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.processing.hotspot.services.HotSpotProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeProvider

class CompartmentHotSpotProvider @Inject constructor(
        private val compartmentGeometryGenerator: CompartmentGeometryGenerator,
        private val shapeProvider: ShapeProvider,
        private val splitResultInterpreter: SplitResultInterpreter
) : HotSpotProvider<Shape, CompartmentHotSpotDefinition> {
    override fun generateShape(hRef: ElementReference<CompartmentHotSpotDefinition>, elements: Map<ElementReference<*>, Element>): Shape? {
        val relation = elements[hRef] as CompartmentHotSpotDefinition

        val result = compartmentGeometryGenerator.generateGeometries(
                relation.splitCompartment ?: relation.base,
                elements,
                elements.filterKeys { it in relation.lineSet }.values.filterIsInstance(GraphicObject::class.java)
        )

        return splitResultInterpreter.getGeometryByIndex(result, relation.index)?.let {
            shapeProvider.toShape(it)
        }
    }
}
