package de.uulm.se.couchedit.jgraphtextensions

import org.jgrapht.Graph
import org.jgrapht.GraphTests
import java.util.*
import kotlin.collections.ArrayList

/**
 * An implementation of Harry Hsu's
 * [transitive reduction algorithm](https://en.wikipedia.org/wiki/Transitive_reduction).
 *
 * cf.
 * [Harry Hsu. "An algorithm for finding a minimal equivalent graph of a digraph.", Journal of the ACM, 22(1):11-16, January 1975.](http://projects.csail.mit.edu/jacm/References/hsu1975:11.html)
 *
 * This is a port from a python example by Michael Clerx, posted as an answer to a question about
 * [transitive reduction algorithm pseudocode](http://stackoverflow.com/questions/1690953/transitive-reduction-algorithm-pseudocode)
 * on [Stack Overflow](http://stackoverflow.com)
 *
 * Originally [org.jgrapht.alg.TransitiveReduction], ported to Kotlin and extended with two parameters that allow:
 *  * Mapping an edge to other source and target vertices than it is in the original graph. This is important for using
 *    a data model where every edge also has an intermediary node associated with it (so that edges can also be relation
 *    targets)
 *
 *  * Specifying whether an edge will be removed if it is not part of the transitive reduction.
 *
 * Original Java source code can be found at:
 * https://github.com/jgrapht/jgrapht/blob/master/jgrapht-core/src/main/java/org/jgrapht/alg/TransitiveReduction.java
 * 
 * ORIGINAL LICENSE BELOW
 * 
 * (C) Copyright 2015-2020, by Christophe Thiebaud and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */

object TransitiveReduction {
    /**
     * This method will remove all transitive edges from the graph passed as input parameter.
     *
     * You may want to clone the graph before, as transitive edges will be pitilessly removed.
     *
     * @param directedGraph the directed graph that will be reduced transitively
     * @param edgeToAdjacency Function mapping an edge in the graph to a pair of vertices that it connects.
     *                        If a null value is returned, the source and target nodes of the edge will be used directly.
     *                        Only edges giving a non-null value in this function will potentially be deleted.
     * @param isEdgeRelevant  Only if this function returns true for a graph edge, the edge will be included in the
     *                        calculation of reduced edges.
     * @param isEdgeRemovable Only if this function returns true for a graph edge, the edge will be removed when the
     *                        connection between its two [edgeToAdjacency] nodes is not in the adjacency matrix.
     * @param V the graph vertex type
     * @param E the graph edge type
     */
    fun <V, E> reduce(
            directedGraph: Graph<V, E>,
            edgeToAdjacency: (E) -> Pair<V, V>?,
            isEdgeRelevant: (E) -> Boolean,
            isEdgeRemovable: (E) -> Boolean
    ) {
        GraphTests.requireDirected(directedGraph, "Graph must be directed")

        val vertices = ArrayList<V>(directedGraph.vertexSet())

        val n = vertices.size

        val matrix = arrayOfNulls<BitSet>(n)
        for (i in matrix.indices) {
            matrix[i] = BitSet(n)
        }

        // Store the edges that are to be removed if the adjacency between the two vertices in the key pair is to be
        // removed
        val removableEdges = mutableMapOf<Pair<Int, Int>, MutableSet<E>>()

        // initialize matrix with zeros
        // 'By default, all bits in the set initially have the value false.'
        // cf. http://docs.oracle.com/javase/7/docs/api/java/util/BitSet.html

        // initialize matrix with edges
        val edges = directedGraph.edgeSet()
        for (edge in edges) {
            if (!isEdgeRelevant(edge)) {
                continue
            }

            val adjacent = edgeToAdjacency(edge)

            val (v1, v2) = adjacent ?: Pair(
                    directedGraph.getEdgeSource(edge),
                    directedGraph.getEdgeTarget(edge)
            )

            val v1Index = vertices.indexOf(v1)
            val v2Index = vertices.indexOf(v2)

            matrix[v1Index]!!.set(v2Index)

            if (isEdgeRemovable(edge)) {
                removableEdges.getOrPut(Pair(v1Index, v2Index)) {
                    mutableSetOf()
                }.add(edge)
            }
        }

        // create path matrix from original matrix

        transformToPathMatrix(matrix)

        // create reduced matrix from path matrix

        transitiveReduction(matrix)

        // remove edges from the DirectedGraph which are not in the reduced
        // matrix

        for ((index, removable) in removableEdges) {
            if (!matrix[index.first]!!.get(index.second)) {
                directedGraph.removeAllEdges(removable)
            }
        }
    }

    /**
     * The matrix passed as input parameter will be transformed into a path matrix.
     *
     *
     *
     * This method is package visible for unit testing, but it is meant as a private method.
     *
     *
     * @param matrix the original matrix to transform into a path matrix
     */
    internal fun transformToPathMatrix(matrix: Array<BitSet?>) {
        // compute path matrix
        for (i in matrix.indices) {
            for (j in matrix.indices) {
                if (i == j) {
                    continue
                }
                if (matrix[j]!!.get(i)) {
                    for (k in matrix.indices) {
                        if (!matrix[j]!!.get(k)) {
                            matrix[j]!!.set(k, matrix[i]!!.get(k))
                        }
                    }
                }
            }
        }
    }

    /**
     * The path matrix passed as input parameter will be transformed into a transitively reduced
     * matrix.
     *
     *
     *
     * This method is package visible for unit testing, but it is meant as a private method.
     *
     *
     * @param pathMatrix the path matrix to reduce
     */
    internal fun transitiveReduction(pathMatrix: Array<BitSet?>) {
        // transitively reduce
        for (j in pathMatrix.indices) {
            for (i in pathMatrix.indices) {
                if (pathMatrix[i]!!.get(j)) {
                    for (k in pathMatrix.indices) {
                        if (pathMatrix[j]!!.get(k)) {
                            pathMatrix[i]!!.set(k, false)
                        }
                    }
                }
            }
        }
    }
}
