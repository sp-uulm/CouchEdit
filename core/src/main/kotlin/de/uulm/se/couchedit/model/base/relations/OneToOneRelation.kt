package de.uulm.se.couchedit.model.base.relations

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.util.collection.joinToString
import de.uulm.se.couchedit.util.collection.sortedPairOf

/**
 * Base class for arbitrary relations between two [Element]s.
 * Those two [Element]s [a] and [b] are identified by their globally unique IDs and no references are included to
 * avoid having multiple competing instances when propagating Relation diffs.
 */
abstract class OneToOneRelation<out A : Element, out B : Element>(val a: ElementReference<A>, val b: ElementReference<B>)
    : Relation<A, B>(setOf(a), setOf(b)) {

    override val id: String
        get() {
            return if (this.isDirected) {
                this.javaClass.simpleName + "_" + this.a.id + "_" + this.b.id
            } else {
                this.javaClass.simpleName + "_" + sortedPairOf(this.a.id, this.b.id).joinToString("_")
            }
        }

    /**
     * This [OneToOneRelation] is equivalent to [other] iff:
     * * If the classes are the same
     *
     * ### For undirected relations:
     * a and b of this relation are also contained in the [other] relation, the order does not matter
     *
     * ### For directed relations:
     * a and b of this relation are equal to the ones of the [other] relation.
     */
    override fun contentEquivalent(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || javaClass != other.javaClass) return false

        val relation = other as? OneToOneRelation<*, *> ?: return false

        if (this.id != relation.id) {
            return false
        }

        if (this.isDirected) {
            val aEquals = this.a == relation.a
            val bEquals = this.b == relation.b
            return aEquals && bEquals
        }

        return listOf(this.a, this.b).containsAll(listOf(relation.a, relation.b))
    }

    abstract override fun copy(): OneToOneRelation<A, B>

    override fun toString(): String = "${this.javaClass.name}(from=$a,to=$b,directed=$isDirected)"
}
