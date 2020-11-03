package de.uulm.se.couchedit.serialization.controller

import de.uulm.se.couchedit.serialization.model.ElementCollection

interface Persister {
    fun persist(elements: ElementCollection, path: String)

    fun load(path: String): ElementCollection
}
