package de.uulm.se.couchedit.processing.common.controller.factory

import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import java.util.concurrent.ExecutorService

interface ProcessorModificationPortFactory {
    fun createProcessorModificationPort(
            processor: Processor,
            diffCollectionFactory: DiffCollectionFactory,
            centralExecutor: ExecutorService
    ): ModificationPort
}
