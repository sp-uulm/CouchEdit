package de.uulm.se.couchedit.model.compartment

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

/**
 * Marker interface for [Element]s belonging to the Compartments mechanism's data model
 */
interface CompartmentElement : Element {
    val base: ElementReference<GraphicObject<*>>

    val splitCompartment: ElementReference<CompartmentElement>?

    val lineSet: Set<ElementReference<GraphicObject<*>>>

    val index: CompartmentIndex
}
