package de.uulm.se.couchedit.export.model.graphic

import de.uulm.se.couchedit.export.model.SerProbabilityInfo
import de.uulm.se.couchedit.export.model.SerializableElement
import de.uulm.se.couchedit.export.model.graphic.shapes.SerializableShape

class SerPrimitiveGraphicObject : SerializableElement {
    override var id: String? = null

    override var probability: SerProbabilityInfo? = null

    var shape: SerializableShape? = null

    var z: MutableList<Int>? = null
}
