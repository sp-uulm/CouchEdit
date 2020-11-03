package de.uulm.se.couchedit.processing.spatial.services.geometric

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition
import de.uulm.se.couchedit.processing.hotspot.services.AggregateHotSpotProvider
import de.uulm.se.couchedit.util.extensions.getTyped

/**
 * Service to extract a [Shape] from a given [ShapedElement] and its related elements.
 */
@Singleton
class DelegatingShapeExtractor @Inject internal constructor(
        private val hotSpotShapeGenerator: AggregateHotSpotProvider,
        private val shapeBoundsCalculator: ShapeBoundsCalculator
) : ShapeExtractor {
    /**
     * Returns the [Shape] from the given referenced object [r] and its related objects (an instance of [r] being contained
     * in [elements]).
     *
     * Returns null if the Element referenced by [r] can currently not provide a Shape (e.g. because its preconditions
     * are no longer valid)
     */
    override fun <S : Shape> extractShape(r: ElementReference<ShapedElement<S>>, elements: Map<ElementReference<*>, Element>): S? {
        return when {
            r.referencesType(GraphicObject::class.java) -> {
                extractFromGraphicObject(elements.getTyped(r.asType())!!)
            }
            r.referencesType(HotSpotDefinition::class.java) -> {
                val typedR = r.asType<HotSpotDefinition<S>>()

                hotSpotShapeGenerator.generateShape(typedR, elements)
            }
            else -> throw IllegalArgumentException("Class ${r.type} cannot provide a shape!")
        }
    }

    override fun <S : Shape> extractBoundingBox(
            r: ElementReference<ShapedElement<S>>,
            elements: Map<ElementReference<*>, Element>
    ): Rectangle? {
        val shape = this.extractShape(r, elements) ?: return null

        return shapeBoundsCalculator.getBoundingBox(shape, r.id)
    }

    private fun <S : Shape> extractFromGraphicObject(go: GraphicObject<S>): S {
        return go.shape
    }
}
