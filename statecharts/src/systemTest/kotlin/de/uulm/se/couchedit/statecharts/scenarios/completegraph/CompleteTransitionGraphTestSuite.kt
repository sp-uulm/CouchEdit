package de.uulm.se.couchedit.statecharts.scenarios.completegraph

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTestSuite

class CompleteTransitionGraphTestSuite : CouchEditTestSuite() {
    override fun getTestInstances(): List<CouchEditTest> {
        val tests = mutableListOf<CouchEditTest>()

        for (i in (1..4)) {
            for (j in 1..4) {
                if (i == j && i == 1) {
                    continue
                }

                tests.add(CompleteTransitionGraphTest(i, j))
            }

        }

        return tests.shuffled()
    }
}
