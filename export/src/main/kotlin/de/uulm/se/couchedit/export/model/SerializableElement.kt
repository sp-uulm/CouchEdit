package de.uulm.se.couchedit.export.model

interface SerializableElement: SerializableObject {
    var id: String?

    var probability: SerProbabilityInfo?
}
