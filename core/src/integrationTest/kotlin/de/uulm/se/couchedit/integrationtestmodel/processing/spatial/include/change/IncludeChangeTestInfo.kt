package de.uulm.se.couchedit.integrationtestmodel.processing.spatial.include.change

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

data class IncludeChangeTestInfo(
        val outerGridSizeX: Int,
        val outerGridSizeY: Int,
        val outerElementCount: Int,
        val innerGridSizeX: Int,
        val innerGridSizeY: Int,
        val innerElementCount: Int
): AbstractTestDetails()
