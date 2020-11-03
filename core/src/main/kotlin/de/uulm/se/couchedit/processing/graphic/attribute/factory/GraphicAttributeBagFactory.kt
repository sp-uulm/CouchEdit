package de.uulm.se.couchedit.processing.graphic.attribute.factory

import com.google.inject.Inject
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.processing.attribute.factory.SubAttributeBagFactory
import de.uulm.se.couchedit.processing.common.services.datastore.IdGenerator

class GraphicAttributeBagFactory @Inject constructor(private val idGenerator: IdGenerator) : SubAttributeBagFactory<GraphicObject<*>> {
    override fun availableBagTypes(element: GraphicObject<*>): Set<Class<out AttributeBag>> {
        return availableBagTypesForShape.filterKeys { it.isAssignableFrom(element.shapeClass) }.flatMap { (_, set) -> set }.toSet()
    }

    /**
     * Creates an [AttributeBag] of the given [bagClass] that is applicable for the given [element].
     */
    override fun createBag(bagClass: Class<out AttributeBag>, element: GraphicObject<*>): AttributeBag? {
        if (bagClass !in availableBagTypes(element)) {
            return null
        }

        if (LineAttributes::class.java.isAssignableFrom(bagClass)) {
            return LineAttributes(idGenerator.generate(LineAttributes::class.java))
        }

        return null
    }

    companion object {
        private val availableBagTypesForShape = mapOf(
                Line::class.java to setOf(
                        LineAttributes::class.java
                )
        )
    }
}
