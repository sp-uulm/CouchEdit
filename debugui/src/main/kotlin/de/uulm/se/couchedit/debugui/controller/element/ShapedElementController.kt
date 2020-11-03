package de.uulm.se.couchedit.debugui.controller.element

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSGeometryProvider
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.util.extensions.ref
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class ShapedElementController : Controller() {
    private val shapeExtractor: ShapeExtractor by di()

    private val geometryProvider: JTSGeometryProvider by di()

    private var serviceCaller: ServiceCaller? = null

    val modelRepository = SimpleObjectProperty<ModelRepository?>()

    val element = SimpleObjectProperty<ElementReference<ShapedElement<*>>?>()

    val mainGeometryText = SimpleStringProperty("")

    val hotSpotsText = SimpleStringProperty("")

    init {
        modelRepository.addListener { _, _, newValue ->
            serviceCaller = newValue?.let {
                ServiceCaller(newValue)
            }

            loadInfo()
        }

        element.addListener { _, _, _ ->
            loadInfo()
        }
    }

    fun loadInfo() {
        // 1. generate text for base shape
        element.value?.let {
            mainGeometryText.value = generateJTSGeometryText(it)

            // 2. get all HotSpotDefinitions attached

            val hotSpotDefinitions = modelRepository.value?.getRelationsFromElement(
                    it.id,
                    HotSpotDefinition::class.java,
                    true
            )?.values

            hotSpotsText.value = hotSpotDefinitions?.joinToString(",\n", "GEOMETRYCOLLECTION(\n", "\n)", -1, "")
            { hsd ->
                generateJTSGeometryText(hsd.ref())
            }
        }
    }

    private fun generateJTSGeometryText(refShapedElement: ElementReference<ShapedElement<Shape>>): String {
        val shape = serviceCaller?.call(refShapedElement, shapeExtractor::extractShape)
                ?: return "<Service caller not set!>"

        val geometry = geometryProvider.toGeometry(shape, refShapedElement.id)

        return geometry.toString()
    }
}
