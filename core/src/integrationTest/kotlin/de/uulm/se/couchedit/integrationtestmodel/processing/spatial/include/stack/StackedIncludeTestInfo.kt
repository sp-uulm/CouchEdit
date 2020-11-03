package de.uulm.se.couchedit.integrationtestmodel.processing.spatial.include.stack

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

data class StackedIncludeTestInfo(
        val gridSizeX: Int,
        val gridSizeY: Int,
        val stackDepth: Int,
        val movedElementsPerStack: Int
): AbstractTestDetails()
