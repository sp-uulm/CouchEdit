package de.uulm.se.couchedit.model.spatial.relations

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.graphic.ShapedElement

/**
 * Base class for [OneToOneRelation]s representing how Elements are located to each other.
 */
abstract class SpatialRelation(from: ElementReference<ShapedElement<*>>, to: ElementReference<ShapedElement<*>>)
    : OneToOneRelation<ShapedElement<*>, ShapedElement<*>>(from, to) {
    /**
     * Spatial relations are explicit by default as they are based on geometric facts.
     */
    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    abstract override fun copy(): SpatialRelation
}
