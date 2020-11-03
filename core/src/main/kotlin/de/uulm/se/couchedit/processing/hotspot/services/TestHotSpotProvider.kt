package de.uulm.se.couchedit.processing.hotspot.services

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.hotspot.TestHotSpotDefinition
import de.uulm.se.couchedit.util.extensions.getTyped

class TestHotSpotProvider : HotSpotProvider<Rectangle, TestHotSpotDefinition> {
    override fun generateShape(hRef: ElementReference<TestHotSpotDefinition>, elements: Map<ElementReference<*>, Element>): Rectangle {
        val definitionElement = elements.getTyped(hRef)!!

        val rectangleGraphicObject = elements.getTyped(definitionElement.a)!!

        return rectangleGraphicObject.shape.copy().apply { w *= 0.5 }
    }
}
