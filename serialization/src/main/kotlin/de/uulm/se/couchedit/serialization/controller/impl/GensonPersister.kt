package de.uulm.se.couchedit.serialization.controller.impl

import com.google.inject.Inject
import com.google.inject.Singleton
import com.owlike.genson.Genson
import de.uulm.se.couchedit.serialization.controller.Persister
import de.uulm.se.couchedit.serialization.model.ElementCollection
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter

@Singleton
internal class GensonPersister @Inject constructor(private val gson: Genson) : Persister {
    override fun persist(elements: ElementCollection, path: String) {
        val bw = BufferedWriter(FileWriter(path))

        gson.serialize(elements, bw)

        bw.close()
    }

    override fun load(path: String): ElementCollection {
        val br = BufferedReader(FileReader(path))

        return gson.deserialize(br, ElementCollection::class.java)
    }
}
