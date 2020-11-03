package de.uulm.se.couchedit.processing.common.controller.factory

import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.controller.ProductionProcessorModificationPort
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import java.util.concurrent.ExecutorService

class ProductionProcessorModificationPortFactory : ProcessorModificationPortFactory {
    override fun createProcessorModificationPort(
            processor: Processor,
            diffCollectionFactory: DiffCollectionFactory,
            centralExecutor: ExecutorService
    ): ModificationPort {
        return ProductionProcessorModificationPort(processor, diffCollectionFactory, centralExecutor)
    }
}
