package de.uulm.se.couchedit.systemtestutils.controller.modbus

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusStateCacheImpl
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.systemtestutils.controller.processing.model.TrackableDiffCollectionWrapper

@ProcessorScoped
class TestModificationBusStateCacheImpl @Inject constructor(
        modelRepository: ModelRepository,
        applicator: Applicator
) : ModificationBusStateCacheImpl(modelRepository, applicator) {
    val cachedDiffCollections = mutableSetOf<String>()

    override fun onBeforeProcess(diffs: TimedDiffCollection) {
        synchronized(this) {
            if (diffs is TrackableDiffCollectionWrapper) {
                cachedDiffCollections.addAll(diffs.ids)
            }
        }
    }

    override fun getCacheContents(): TimedDiffCollection {
        synchronized(this) {
            //println("MBSC - Passing Cache Contents with IDs = $cachedDiffCollections")

            return TrackableDiffCollectionWrapper(cachedDiffCollections.toSet(), super.getCacheContents())
        }
    }
}
