package de.uulm.se.couchedit.model.graphic.elements

import de.uulm.se.couchedit.model.base.AbstractElement
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.shapes.Shape

/**
 * GraphicObject is the base class for everything the visual editor part of the system displays.
 * Those graphic primitives are universal to all modeling languages and are transformed into their semantic meaning
 * later in the system processing.
 *
 * On the basic, most generic level, GraphicObjects are handled by their [shape] (for example for calculating spatial
 * relations).
 * To the user, they are displayed by their [representation], which can consist of multiple graphic primitives, however
 * those are meaningless in the processing by default.
 */
abstract class GraphicObject<T : Shape>(override val id: String, override val shapeClass: Class<out T>) : ShapedElement<T>, AbstractElement() {
    /**
     * The outline by which the element is handled by the processing system
     */
    abstract val shape: T

    /**
     * Z-Order of [GraphicObject]s is handled as follows:
     * The default implicit z is 0.
     *
     * When an item is moved back / forth, it can have positive or negative
     * z elements. The default implicit z is again 0, so an element with
     * z `[4][1]` is ordered in front of the element with z `[4]` and an element with z `[4][-1]` is ordered in back of
     * the element with z `[4]`.
     * This way, elements can be ordered arbitrarily, without having to incur more Diffs than necessary.
     * The [de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf] relation always takes
     * precedence, i.e. arbitrary Z-Order relations between elements of different parents are not supported.
     *
     * When two elements have exactly the same z, their z-order is arbitrary (e.g. in order of insertion).
     *
     * In the front end, it should be taken care of that the z component is not nested deeper than necessary, however
     * nodes within a common parent should share a common z prefix so that their relation towards other nodes
     * will not be destroyed when the parent changes
     */
    var z = mutableListOf<Int>()

    /**
     * By default, GraphicObjects are marked as [ProbabilityInfo.Explicit] because most commonly, they are created by the
     * user.
     */
    override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit

    abstract override fun copy(): GraphicObject<T>

    /**
     * Sets all values (except the ID) of this [GraphicObject] to the values of [other].
     */
    abstract fun setFrom(other: GraphicObject<in T>)

    /**
     * Checks whether the [z] of the [other] element is the same, as well as the [shapeClass].
     *
     * Subclasses need to override this method to include their individual properties that must be equal
     */
    override fun contentEquivalent(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as GraphicObject<*>

        if (this.shapeClass != other.shapeClass) return false

        return (other as? GraphicObject<*>)?.z == this.z
    }

    /**
     * Helper function to convert this Element's [z] to a dot-separated String.
     */
    protected fun zToString() = this.z.joinToString(".")
}
