package de.uulm.se.couchedit.export.model

abstract class SerOneToOneRelation: SerializableElement {
    override var id: String? = null

    override var probability: SerProbabilityInfo? = null

    var a: SerElementReference? = null
    var b: SerElementReference? = null
}
