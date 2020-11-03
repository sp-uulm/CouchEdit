package de.uulm.se.couchedit.serialization.model

import de.uulm.se.couchedit.export.model.SerTimestamp
import de.uulm.se.couchedit.export.model.SerializableElement

data class ElementInfo(var element: SerializableElement? = null, var timestamp: SerTimestamp? = null)
