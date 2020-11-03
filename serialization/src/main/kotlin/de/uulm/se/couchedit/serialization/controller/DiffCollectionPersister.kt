package de.uulm.se.couchedit.serialization.controller

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.export.context.FromSerializableContext
import de.uulm.se.couchedit.export.context.ToSerializableContext
import de.uulm.se.couchedit.processing.common.model.diffcollection.PreparedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.serialization.controller.conversion.ElementCollectionConverter
import de.uulm.se.couchedit.serialization.controller.conversion.IdLoadPrefixer
import de.uulm.se.couchedit.serialization.controller.conversion.IdMapper

/**
 * This class allows the saving of a DiffCollection to disk and loading a DiffCollection back from a file.
 *
 * It also supports adding prefixes to the IDs of the elements to be loaded / saved. This is important as it is necessary
 * to fix the following conflicts:
 * * Loaded objects vs. already present objects: If two Elements have the same ID, to the whole application they are
 *   a representation of the same thing. This would lead to very unwanted behavior if an Element in the file to be
 *   loaded collides with an already stored Element.
 *   Thus, the [IdMapper] builds an entirely new structure of IDs specific to the current save process.
 * * Loaded objects vs. objects previously loaded from the same file: If the same file is loaded twice, the IDs would
 *   again be the same, leading to similar collisions. For that, we prefix the loaded IDs again with a unique prefix
 *   for each load process. This also prevents crashing the system with manually edited files.
 *   TODO: Potentially there is a better solution for the latter case / this "overwriting" is wanted??
 */
@Singleton
class DiffCollectionPersister @Inject constructor(
        private val elementCollectionConverter: ElementCollectionConverter,
        private val persister: Persister
) {
    /**
     * Stores the given [diffs] as a file. While doing this, remove the [prefix] from all Element IDs where it is present.
     */
    fun persistDiffCollection(diffs: TimedDiffCollection, path: String, prefix: String) {
        val idMapper = IdMapper(prefix)

        val toSerializableContext = ToSerializableContext(idMapper)

        val elementCollection = elementCollectionConverter.convertDiffCollection(diffs, toSerializableContext)

        this.persister.persist(elementCollection, path)
    }

    fun loadDiffCollection(path: String, prefix: String): PreparedDiffCollection {
        val elementCollection = this.persister.load(path)

        val fromSerializableContext = FromSerializableContext(IdLoadPrefixer(prefix))

        return elementCollectionConverter.convertElementCollection(elementCollection, fromSerializableContext)
    }
}
