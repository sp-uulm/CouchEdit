package de.uulm.se.couchedit.statecharts.testdata

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line

/**
 * Concrete Syntax representation of a [GraphicObject] which (in combination with an outer area RoundedRectangle)
 * constitutes an Abstract Syntax Orthogonal State.
 */
data class OrthogonalStateLineRepresentation(
        val map: Map<ElementRole, Element>
) : Map<OrthogonalStateLineRepresentation.ElementRole, Element> by map {
    @Suppress("UNCHECKED_CAST")
    val line = map[ElementRole.LINE] as GraphicObject<Line>

    enum class LineOrientation {
        HORIZONTAL,
        VERTICAL
    }

    enum class ElementRole {
        LINE,
        ATTRIBUTES,
        ATTRIBUTES_FOR
    }
}
