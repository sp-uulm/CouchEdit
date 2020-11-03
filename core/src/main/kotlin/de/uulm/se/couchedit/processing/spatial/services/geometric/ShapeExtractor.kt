package de.uulm.se.couchedit.processing.spatial.services.geometric

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape

/**
 * Interface for a Service that is able to generate [Shape] objects from [ShapedElement]s
 *
 * [Interface needed so that DI can resolve circular dependencies]
 */
interface ShapeExtractor {
    fun <S : Shape> extractShape(r: ElementReference<ShapedElement<S>>, elements: Map<ElementReference<*>, Element>): S?

    fun <S : Shape> extractBoundingBox(r: ElementReference<ShapedElement<S>>, elements: Map<ElementReference<*>, Element>): Rectangle?
}
