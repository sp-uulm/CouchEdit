package de.uulm.se.couchedit.statecharts.scenarios.include.change

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class ChildStateChangeTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        return (1..20).map { ChildStateChangeTest(it) }.shuffled()
    }
}
