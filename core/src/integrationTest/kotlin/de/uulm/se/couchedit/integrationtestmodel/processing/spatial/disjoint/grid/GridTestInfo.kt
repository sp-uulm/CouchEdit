package de.uulm.se.couchedit.integrationtestmodel.processing.spatial.disjoint.grid

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

data class GridTestInfo(
        val gridSizeX: Int,
        val gridSizeY: Int,
        val totalNumberOfElements: Int,
        val numberOfMovedElements: Int
) : AbstractTestDetails()
