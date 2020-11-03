package de.uulm.se.couchedit.processing.spatial.disjoint.grid

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class RectangleGridTestSuite : CouchEditTestSuite() {
    override fun getDryRunTestInstances(): List<CouchEditTest> {
        return listOf(RectangleGridTest(3, 3, 1))
    }

    override fun getTestInstances(): List<CouchEditTest> {
        return (5..20).map { size ->
            RectangleGridTest(size, size, 10)
        }.shuffled()
    }
}
