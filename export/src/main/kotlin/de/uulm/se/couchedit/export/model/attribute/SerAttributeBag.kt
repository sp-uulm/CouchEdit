package de.uulm.se.couchedit.export.model.attribute

import de.uulm.se.couchedit.export.model.SerProbabilityInfo
import de.uulm.se.couchedit.export.model.SerializableElement

class SerAttributeBag : SerializableElement {
    override var id: String? = null
    override var probability: SerProbabilityInfo? = null

    var bagClass: String? = null

    var bagValues: List<SerAttributeBagItem>? = emptyList()
}
