package de.uulm.se.couchedit.export.model.graphic.shapes

class SerPolygon: SerializableShape {
    var outerBorder: List<SerPoint>? = null
    var holes: List<List<SerPoint>>? = null
}
