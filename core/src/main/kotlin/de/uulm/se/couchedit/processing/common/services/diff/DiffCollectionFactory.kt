package de.uulm.se.couchedit.processing.common.services.diff

import com.google.inject.Singleton
import de.uulm.se.couchedit.processing.common.model.diffcollection.*

@Singleton
class DiffCollectionFactory {
    fun createPreparedDiffCollection(): PreparedDiffCollection = PreparedDiffCollection()

    fun createTimedDiffCollection(): TimedDiffCollection = TimedDiffCollectionImpl()
    fun createMutableTimedDiffCollection(): MutableTimedDiffCollection = MutableTimedDiffCollectionImpl()
}
