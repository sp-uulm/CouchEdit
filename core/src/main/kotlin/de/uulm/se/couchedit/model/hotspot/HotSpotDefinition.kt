package de.uulm.se.couchedit.model.hotspot

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.relations.Relation
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Shape

/**
 * A HotSpotDefinition is a Relation from a GraphicObject a to one or more ShapedElements that specifies parameters
 * for the calculation of a dynamic shape based on the properties of a and all Elements in the b set as well as the
 * properties stored in the HotSpotDefinition itself.
 *
 * To obtain the effective shape specified by the HotSpotDefinition, the ShapeExtractor (which delegates to the
 * AggregateHotSpotProvider) is used.
 *
 * @param baseElement GraphicObject that this HotSpotDefinition specifies a HotSpot for. This is used to quickly get
 *                    all HotSpots of a certain Element without having to traverse all HotSpotDefinitions.
 *                    The baseElement -> HotSpotDefinition relation is always to be handled like it was an
 *                    [de.uulm.se.couchedit.model.spatial.relations.Include]  relation.
 * @param parentHotSpot The HotSpot that contains this HotSpot.
 *                      The parentHotSpot -> HotSpotDefinition relation is always to be handled like it was an
 *                      [de.uulm.se.couchedit.model.spatial.relations.Include] relation. Otherwise, the HotSpot can just
 *                      be put in the [b] set.
 * @param b All other [ShapedElement]s that influence the generated HotSpots.
 */
abstract class HotSpotDefinition<S: Shape>(
        baseElement: ElementReference<GraphicObject<*>>,
        parentHotSpot: ElementReference<HotSpotDefinition<*>>?,
        b: Set<ElementReference<ShapedElement<*>>>,
        override val shapeClass: Class<S>
) : Relation<ShapedElement<*>, ShapedElement<*>>(
        parentHotSpot?.let { setOf<ElementReference<ShapedElement<*>>>(baseElement, parentHotSpot) } ?: setOf(baseElement),
        b
), ShapedElement<S> {
    override val id = "${this::class.java.simpleName}_${refSetToIdString(aSet)}_${refSetToIdString(bSet)}"

    override val isDirected = true

    private fun refSetToIdString(set: Set<ElementReference<*>>): String {
        return set.map(ElementReference<*>::id).joinToString("_", "[", "]")
    }
}
