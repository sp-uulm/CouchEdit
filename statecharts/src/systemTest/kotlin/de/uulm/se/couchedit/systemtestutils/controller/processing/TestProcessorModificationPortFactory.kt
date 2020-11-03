package de.uulm.se.couchedit.systemtestutils.controller.processing

import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.controller.factory.ProcessorModificationPortFactory
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.systemtestutils.controller.manager.TestModificationPortRegistry
import java.util.concurrent.ExecutorService

class TestProcessorModificationPortFactory(private val registry: TestModificationPortRegistry) : ProcessorModificationPortFactory {
    override fun createProcessorModificationPort(
            processor: Processor,
            diffCollectionFactory: DiffCollectionFactory,
            centralExecutor: ExecutorService
    ): ModificationPort {
        val ret = TestProcessorModificationPort(
                processor,
                diffCollectionFactory,
                centralExecutor,
                registry
        )

        registry.registerPort(ret)

        return ret
    }
}
