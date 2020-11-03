package de.uulm.se.couchedit.export.exceptions

import de.uulm.se.couchedit.export.model.SerializableObject

class InstantiationImpossible(
        val sourceClass: Class<out SerializableObject>,
        val targetClass: Class<out Any>,
        val reason: String = ""
) : ConvertFromSerializableException() {
    override val message = "InstantiationImpossible: Cannot convert ${sourceClass.simpleName} because ${targetClass.name} " +
            "cannot be instantiated - $reason"
}
