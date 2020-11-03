package de.uulm.se.couchedit.processing.spatial.intersect

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class RoundedRectangleCascadingIntersectTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (5..20).map { size ->
            // use the same size steps as in other test suites
            val elementCount = size * size

            RoundedRectangleCascadingIntersectTest(elementCount, 10)
        }
    }
}
