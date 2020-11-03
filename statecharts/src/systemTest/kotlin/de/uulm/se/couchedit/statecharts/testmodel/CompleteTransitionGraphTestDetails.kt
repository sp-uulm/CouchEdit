package de.uulm.se.couchedit.statecharts.testmodel

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

class CompleteTransitionGraphTestDetails(
        val gridSizeX: Int,
        val gridSizeY: Int,
        val totalStateCount: Int,
        val totalConnectionCount: Int
) : AbstractTestDetails()
