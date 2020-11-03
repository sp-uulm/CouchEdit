package de.uulm.se.couchedit.jtsextensions.overlay


/*
 * Original code from JTS Test Builder, converted to Kotlin:
 * https://github.com/locationtech/jts/blob/master/modules/app/src/main/java/org/locationtech/jtstest/function/PolygonOverlayFunctions.java
 *
 * ORIGINAL LICENSE BELOW
 * 
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.noding.snapround.GeometryNoder
import org.locationtech.jts.operation.polygonize.Polygonizer


object PolygonOverlayFunctions {
    fun overlaySnapRounded(g1: Geometry, g2: Geometry?, precisionTol: Double): Geometry {
        val pm = PrecisionModel(precisionTol)
        val geomFact = g1.factory

        val lines = LinearComponentExtracter.getLines(g1)
        // add second input's linework, if any
        if (g2 != null)
            LinearComponentExtracter.getLines(g2, lines)
        val nodedLinework = GeometryNoder(pm).node(lines)

        // union the noded linework to remove duplicates
        val nodedDedupedLinework = geomFact.buildGeometry(nodedLinework).union()

        // polygonize the result
        val polygonizer = Polygonizer()

        /*
         * Adds all LineStrings contained in the sub-geometries of the given linework to the polygonizer.
         */
        polygonizer.add(nodedDedupedLinework)
        val polys = polygonizer.getPolygons()

        // convert to collection for return
        val polyArray = GeometryFactory.toPolygonArray(polys)
        return geomFact.createGeometryCollection(polyArray)
    }

}
