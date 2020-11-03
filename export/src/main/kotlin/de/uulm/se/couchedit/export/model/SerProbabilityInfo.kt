package de.uulm.se.couchedit.export.model

sealed class SerProbabilityInfo : SerializableObject {
    class SerGenerated : SerProbabilityInfo() {
        var probability: Double? = null
    }

    @Suppress("CanSealedSubClassBeObject") // required for serialization and deserialization with Java libraries
    class SerExplicit() : SerProbabilityInfo()
}
