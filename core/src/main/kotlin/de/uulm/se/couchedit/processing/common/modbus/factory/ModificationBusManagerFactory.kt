package de.uulm.se.couchedit.processing.common.modbus.factory

import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusStateCache

interface ModificationBusManagerFactory {
    fun createModificationBusManager(
            initialPorts: Set<ModificationPort>,
            modificationBusStateCache: ModificationBusStateCache
    ): ModificationBusManager
}
