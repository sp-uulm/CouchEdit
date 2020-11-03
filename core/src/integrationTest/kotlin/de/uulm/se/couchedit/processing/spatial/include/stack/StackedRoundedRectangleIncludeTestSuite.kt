package de.uulm.se.couchedit.processing.spatial.include.stack

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class StackedRoundedRectangleIncludeTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (2..7).map { size ->
            StackedRoundedRectangleIncludeTest(size, size, 7)
        }
    }
}
