package de.uulm.se.couchedit.statecharts.scenarios.compartment.grid

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class OrthogonalStateGridTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (2..10).map {
            OrthogonalStateGridTest(it, it, it * 200.0, it * 200.0)
        }.shuffled()
    }
}
