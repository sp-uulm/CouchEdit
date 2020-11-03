package de.uulm.se.couchedit.processing.hotspot.services

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition

/**
 * A HotSpotProvider is a service that handles a certain class [H] of [HotSpotDefinition] and is able to produce the
 * [Shape] defined by this HotSpotDefinition based on its other related elements.
 */
interface HotSpotProvider<out S: Shape, H : HotSpotDefinition<out S>> {
    /**
     * Generate a [Shape] from the [HotSpotDefinition] referenced by [hRef] and its related [elements].
     */
    fun generateShape(hRef: ElementReference<H>, elements: Map<ElementReference<*>, Element>): S?
}
