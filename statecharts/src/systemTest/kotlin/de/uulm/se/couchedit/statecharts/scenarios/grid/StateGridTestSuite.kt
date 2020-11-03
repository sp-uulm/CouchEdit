package de.uulm.se.couchedit.statecharts.scenarios.grid

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class StateGridTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (3..15).map { StateGridTest(it, it, 5, 2, 2, 2, 2) }.shuffled()
    }
}
