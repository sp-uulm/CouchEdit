package de.uulm.se.couchedit.statecharts.testmodel

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

data class OrthogonalStateGridTestDetails(
        val gridSizeX: Int,
        val gridSizeY: Int,
        val numberOfOrthogonalStates: Int
) : AbstractTestDetails() {
}
