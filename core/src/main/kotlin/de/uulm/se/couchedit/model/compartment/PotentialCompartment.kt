package de.uulm.se.couchedit.model.compartment

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject

/**
 * Wrapper for a [CompartmentHotSpotDefinition] which does not cause the HotSpot shape to be calculated.
 * Meant to be used for downstream (abstract syntax aware) processors that can then insert the given [hsd].
 *
 * As in this stage, the [CompartmentHotSpotDefinition.splitCompartment] can also still only be a "potential"
 * Compartment object, this class provides the possibility to specify a [potentialSplitCompartment], which is a
 * reference to the [PotentialCompartment] representing the [CompartmentHotSpotDefinition] while it is not yet actually
 * inserted.
 */
class PotentialCompartment(
        val hsd: CompartmentHotSpotDefinition,
        val potentialSplitCompartment: ElementReference<PotentialCompartment>?
) : Relation<Element, ShapedElement<*>>(
        if (potentialSplitCompartment != null && hsd.splitCompartment != null)
            hsd.aSet.minus(hsd.splitCompartment).plus(potentialSplitCompartment)
        else
            hsd.aSet,
        hsd.bSet
), CompartmentElement {
    override val id = "potential_${hsd.id}" + (potentialSplitCompartment?.let { "_${it.id}" } ?: "")

    override val isDirected = true

    override val base = hsd.base
    override val splitCompartment: ElementReference<CompartmentElement>? = potentialSplitCompartment
            ?: hsd.splitCompartment
    override val lineSet = hsd.lineSet

    override val index = hsd.index

    override var probability: ProbabilityInfo?
        get() = this.hsd.probability
        set(value) {
            this.hsd.probability = value
        }

    override fun copy() = PotentialCompartment(hsd.copy(), potentialSplitCompartment)

    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && (other as? PotentialCompartment)?.hsd?.contentEquivalent(this.hsd) == true
    }
}
