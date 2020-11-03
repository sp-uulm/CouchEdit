package de.uulm.se.couchedit.util.extensions

import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Shape

/**
 * Convenience function to cast any ElementReference<ShapedElement<*>> to an ElementReference<Shape>.
 */
fun ElementReference<ShapedElement<*>>.asGenericShape(): ElementReference<ShapedElement<Shape>> {
    /*
     * In theory, this should not be necessary, as the type parameter in ShapedElement is out-projected.
     * Thus, every ShapedElement<*> is also a ShapedElement<Element>:
     * https://kotlinlang.org/docs/reference/generics.html#declaration-site-variance
     *
     * However, in combination with the ServiceCaller function, this seems to be too much for the type system:
     * e.g. in SpatialRelationChecker.getSpatialRelations
     * <code>
     *      Error:(25, 46) Kotlin: Type mismatch: inferred type is
     *      KFunction2<@ParameterName ElementReference<ShapedElement<Nothing>>,
     *                 @ParameterName Map<ElementReference<*>, Element>, Nothing?
     *                >
     *      but (ElementReference<ShapedElement<*>>, Map<ElementReference<*>, Element>) -> Nothing? was expected
     * </code>
     */
    return this
}
