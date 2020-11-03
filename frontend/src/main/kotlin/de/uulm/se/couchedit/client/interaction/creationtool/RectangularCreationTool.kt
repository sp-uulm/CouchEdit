package de.uulm.se.couchedit.client.interaction.creationtool

import com.google.inject.Inject
import de.uulm.se.couchedit.client.interaction.input.CanvasMouseEvent
import de.uulm.se.couchedit.client.interaction.pattern.InteractionPatternCallbacks
import de.uulm.se.couchedit.client.interaction.pattern.SelectRectangularRegion
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.elements.PrimitiveGraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Rectangle
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.processing.common.services.datastore.IdGenerator

/**
 * [CreationTool] that can be used to generate a [PrimitiveGraphicObject] by selecting a rectangular region (via the
 * [SelectRectangularRegion] interaction).
 *
 * Subclasses need to override the [createShapes] function to provide shapes based on the selected rectangular region.
 */
abstract class RectangularCreationTool : CreationTool(), InteractionPatternCallbacks<Rectangle> {
    @Inject
    protected lateinit var idGenerator: IdGenerator

    override fun onClick(e: CanvasMouseEvent) {
        this.activate()

        this.currentInteractionPattern = SelectRectangularRegion(defaultSize(), this)
    }

    override fun onResult(value: Rectangle) {
        check(callbacks != null) { "Callbacks must not be null on interaction finish!" }

        callbacks?.finishCreation(createElements(value))

        this.deactivate()
    }

    override fun onAbort() {
        this.deactivate()
    }

    open fun defaultSize(): Pair<Double, Double> {
        return 80.0 to 50.0
    }

    /**
     * Create [Element]s to be inserted based on the [givenRectangle] selected by the user.
     *
     * Override this method to create Elements other than [PrimitiveGraphicObject]s.
     */
    protected open fun createElements(givenRectangle: Rectangle): Set<Element> {
        return createShapes(givenRectangle).map {
            PrimitiveGraphicObject(idGenerator.generate(PrimitiveGraphicObject::class.java), it)
        }.toSet()
    }

    /**
     * Create Shape objects based on the properties of the [givenRectangle]
     *
     * @param givenRectangle [Rectangle] area as selected by the user
     *
     * @return Set of shapes to be inserted. These are mapped into a [PrimitiveGraphicObject] each by the
     *         [createElements] method with the help of the [idGenerator].
     */
    abstract fun createShapes(givenRectangle: Rectangle): Set<Shape>
}
