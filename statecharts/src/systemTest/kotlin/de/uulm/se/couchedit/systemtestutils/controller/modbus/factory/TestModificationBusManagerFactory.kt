package de.uulm.se.couchedit.systemtestutils.controller.modbus.factory

import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusStateCache
import de.uulm.se.couchedit.processing.common.modbus.factory.ModificationBusManagerFactory
import de.uulm.se.couchedit.systemtestutils.controller.manager.TestModificationPortRegistry

class TestModificationBusManagerFactory(val registry: TestModificationPortRegistry) : ModificationBusManagerFactory {
    override fun createModificationBusManager(
            initialPorts: Set<ModificationPort>,
            modificationBusStateCache: ModificationBusStateCache
    ): ModificationBusManager {
        return ModificationBusManager(initialPorts, modificationBusStateCache) { port, diffs ->
            registry.onDiffCollectionIncoming(port.id, diffs)
        }
    }
}
