package de.uulm.se.couchedit.model.compartment

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Polygon
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition

/**
 * [HotSpotDefinition] defined by a base element, possibly another sub-compartment, and splitting lines crossing it.
 *
 * @param base The Element that contains the CompartmentHotSpotDefinition
 * @param splitCompartment Optional other CompartmentHotSpotDefinition that is subdivided by this Element
 * @param lineSet The set of lines responsible for the compartmentization.
 * @param index The index (x-order, y-order) that this Element has in the set of Compartments defined by this combination
 *              of Elements ([base], [splitCompartment], [lineSet])
 */
class CompartmentHotSpotDefinition(
        override val base: ElementReference<GraphicObject<*>>,
        override val splitCompartment: ElementReference<CompartmentHotSpotDefinition>?,
        override val lineSet: Set<ElementReference<GraphicObject<*>>>,
        override val index: CompartmentIndex
) : CompartmentElement, HotSpotDefinition<Polygon>(
        base,
        splitCompartment,
        lineSet,
        Polygon::class.java
) {
    override val id = super.id + index
    override val isDirected = true

    override var probability: ProbabilityInfo? = null

    override fun copy(): CompartmentHotSpotDefinition {
        return CompartmentHotSpotDefinition(base, splitCompartment, lineSet, index).also {
            it.probability = this.probability
        }
    }

    override fun contentEquivalent(other: Any?): Boolean {
        if (!super.contentEquivalent(other)) return false

        return (other as? CompartmentHotSpotDefinition)?.index == this.index
    }

    override fun toString(): String {
        return "ComHSD(base=${base.id},lines=${lineSet.joinToString(",", "[", "]")}, index=$index, splitCompartment=$splitCompartment)"
    }
}
