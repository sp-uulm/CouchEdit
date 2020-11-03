package de.uulm.se.couchedit.processing.common.model.diffcollection

class MutableTimedDiffCollectionImplTest: MutableTimedDiffCollectionTest() {
    override val systemUnderTest = MutableTimedDiffCollectionImpl().also {
        for (diff in diffList) {
            it.putDiff(diff, versions.getValue(diff.affected.id))
        }
    }
}
