package de.uulm.se.couchedit.processing.common.model.diffcollection

class TimedDiffCollectionImplTest : TimedDiffCollectionTest() {
    override val systemUnderTest = TimedDiffCollectionImpl(diffList.map { it.affected.id to it }.toMap(), versions, emptySet())
}
