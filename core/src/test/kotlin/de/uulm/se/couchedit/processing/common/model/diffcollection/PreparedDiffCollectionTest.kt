package de.uulm.se.couchedit.processing.common.model.diffcollection

import de.uulm.se.couchedit.processing.common.model.ModelDiff

class PreparedDiffCollectionTest : DiffCollectionTest() {
    override val systemUnderTest: DiffCollection = run {
        val ret = PreparedDiffCollection()

        for (diff in diffList) {
            ret.putDiff(diff)
        }

        return@run ret
    }
}
