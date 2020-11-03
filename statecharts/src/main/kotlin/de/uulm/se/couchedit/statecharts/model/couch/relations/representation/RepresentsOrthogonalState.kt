package de.uulm.se.couchedit.statecharts.model.couch.relations.representation

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.statecharts.model.couch.elements.OrthogonalRegion

/**
 * Specifies that the [a] [CompartmentHotSpotDefinition] represents the [b] [OrthogonalRegion].
 */
class RepresentsOrthogonalState(
        a: ElementReference<CompartmentHotSpotDefinition>,
        b: ElementReference<OrthogonalRegion>) : Represents<CompartmentHotSpotDefinition, OrthogonalRegion>(a, b) {
    override fun copy() = RepresentsOrthogonalState(a.asType(), b)
}
