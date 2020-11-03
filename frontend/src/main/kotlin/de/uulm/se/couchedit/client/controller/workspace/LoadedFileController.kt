package de.uulm.se.couchedit.client.controller.workspace

import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasCoordinator
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import de.uulm.se.couchedit.serialization.controller.DiffCollectionPersister
import javafx.beans.property.ReadOnlyStringWrapper
import tornadofx.*
import java.io.File
import java.util.*

class LoadedFileController : Controller() {
    private val modificationBusManager: ModificationBusManager by di()

    private val canvasCoordinator: CanvasCoordinator by di()

    private val persister: DiffCollectionPersister by di()

    private val fileNameProperty = ReadOnlyStringWrapper(null)

    val fileNamePropertyReadOnly = fileNameProperty.readOnlyProperty

    private var fileName: String? by fileNameProperty

    private var loadCount = 0

    val saveable = fileNameProperty.isNotNull

    fun save() {
        synchronized(this) {
            this.saveAsCopy(this.fileName ?: return)
        }
    }

    fun saveAs(path: String) {
        synchronized(this) {
            this.saveAsCopy(path)

            this.fileName = path
        }
    }

    fun saveAsCopy(path: String) {
        synchronized(this) {
            val usedPath = addExtensionIfNecessary(path)

            val dumpCollection = modificationBusManager.exportSystemState()

            persister.persistDiffCollection(dumpCollection, usedPath, generateSavePrefix())
        }
    }

    fun load(path: String) {
        synchronized(this) {
            val loadedCollection = persister.loadDiffCollection(path, generateLoadPrefix())

            this.fileName = path

            this.canvasCoordinator.replaceRepositoryContentsWith(loadedCollection)
        }
    }

    private fun addExtensionIfNecessary(path: String): String {
        val pathObject = File(path)

        if (pathObject.extension == "") {
            return "$path.cjson"
        }

        return path
    }

    /**
     * Generates a prefix for the Elements that are saved to a file.
     * This is necessary so that when loading again, there are no ID conflicts between previously generated Elements
     * and the ones to be loaded. The LoadPrefix alone is not sufficient, because there would be a prefix added every
     * time the file is saved and loaded again, so the Persister builds an entirely new ID structure based on this
     * Prefix.
     */
    private fun generateSavePrefix(): String {
        val date = Calendar.getInstance()

        return "SAV${date.get(Calendar.YEAR)}${date.get(Calendar.MONTH)}${date.get(Calendar.DAY_OF_MONTH)}" +
                "${date.get(Calendar.HOUR_OF_DAY)}${date.get(Calendar.MINUTE)}${date.get(Calendar.SECOND)}" +
                ".${date.get(Calendar.MILLISECOND)}"
    }

    /**
     * Generates a prefix for the currently loaded elements. This is used to avoid conflicts if loading the same file
     * twice (or two files with the same IDs)
     */
    private fun generateLoadPrefix(): String {
        loadCount++
        return "L${loadCount}_"
    }
}
