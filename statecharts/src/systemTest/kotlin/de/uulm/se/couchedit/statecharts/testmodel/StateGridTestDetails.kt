package de.uulm.se.couchedit.statecharts.testmodel

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

data class StateGridTestDetails(
        val gridSizeX: Int,
        val gridSizeY: Int,
        val totalStateCount: Int,
        val numberOfStatesToMove: Int,
        val insertedGridSizeX: Int,
        val insertedGridSizeY: Int,
        val totalInsertedStateCount: Int
) : AbstractTestDetails()
