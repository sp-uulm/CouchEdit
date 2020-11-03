package de.uulm.se.couchedit.statecharts.testmodel

import de.uulm.se.couchedit.testsuiteutils.model.AbstractTestDetails

data class OrthogonalHierarchyConnectionTestDetails(
        val size: Int,
        val stateCount: Int,
        val transitionCount: Int,
        val orthogonalRegionCount: Int
): AbstractTestDetails()
