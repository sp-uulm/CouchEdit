package de.uulm.se.couchedit.statecharts.scenarios.compartment.recursive

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class RecursiveOrthogonalStateTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (1..15).map { RecursiveOrthogonalStateTest(30.0, it) }.shuffled()
    }
}
