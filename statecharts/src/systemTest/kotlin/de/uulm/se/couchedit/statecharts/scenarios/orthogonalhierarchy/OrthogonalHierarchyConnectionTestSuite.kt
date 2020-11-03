package de.uulm.se.couchedit.statecharts.scenarios.orthogonalhierarchy

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class OrthogonalHierarchyConnectionTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (1..5).map {
            OrthogonalHierarchyConnectionTest(it)
        }.shuffled()
    }
}
