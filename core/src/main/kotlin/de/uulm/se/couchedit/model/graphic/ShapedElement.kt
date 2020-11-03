package de.uulm.se.couchedit.model.graphic

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.shapes.Shape

/**
 * Marker interface for all Elements that provide a Shape via the ShapeExtractor
 */
interface ShapedElement<out T : Shape> : Element {
    val shapeClass: Class<out T>
}
