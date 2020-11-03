package de.uulm.se.couchedit.model.graphic.shapes

/**
 * Base interface for all [GraphicPrimitive]s that can be used as a GraphicObject outline.
 * That means, for Shapes it is possible to directly calculate bounding boxes + spatial relations.
 */
interface Shape : GraphicPrimitive
