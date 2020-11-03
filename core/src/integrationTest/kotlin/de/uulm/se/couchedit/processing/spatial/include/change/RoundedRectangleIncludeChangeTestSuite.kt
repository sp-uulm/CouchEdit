package de.uulm.se.couchedit.processing.spatial.include.change

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class RoundedRectangleIncludeChangeTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (5..7).map { size ->
            RoundedRectangleIncludeChangeTest(size, size, 3, 3)
        }
    }
}
