package de.uulm.se.couchedit.debugui.controller.processing

import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import tornadofx.*

/**
 * Controller managing a [DebugLoggingModificationPort] and connecting it to the [ModificationBusManager] of the system.
 *
 * The DiffCollections observed on that ModificationPort are then exposed via the [observedDiffs] property.
 */
class DiffCollectionLogController : Controller() {
    private val modificationBusManager: ModificationBusManager by di()

    private val debugLoggingModificationPort: DebugLoggingModificationPort by di()

    val observedDiffs
        get() = debugLoggingModificationPort.observedDiffs

    /**
     * Connects this controller to the [ModificationBusManager] of the application.
     */
    fun connect() {
        modificationBusManager.registerModificationPort(debugLoggingModificationPort)
    }
}
