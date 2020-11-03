package de.uulm.se.couchedit.processing.common.services.diff

import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import java.util.*

/**
 * Manages the [VectorTimestamp] of all Elements known to a [de.uulm.se.couchedit.processing.common.repository.ModelRepository], as well as the Repository's current time
 * stamp used for modified outgoing elements.
 */
@ProcessorScoped
class VersionManager(val processorId: String = UUID.randomUUID().toString()) {
    private val clock = VectorTimestamp()

    /**
     * Keeps timestamps for the latest versions we got for all known elements.
     *
     * The timestamp of a given element should be the following:
     * - The same as it was in the [DiffCollection] from which it was received
     */
    private val elementVersions = mutableMapOf<String, VectorTimestamp>()

    fun versionOf(id: String): VectorTimestamp? = elementVersions[id]

    /**
     * Checks in which relation the current timestamp of the object in this [VersionManager] stands with the given [timestamp]
     *
     *
     * That means, e.g. if the result is [CausalRelation.STRICTLY_BEFORE], the version which was last stored here
     * is strictly before the version represented by [timestamp].
     */
    fun getRelationFromCurrentVersionToTimestamp(id: String, timestamp: VectorTimestamp): VectorTimestamp.CausalRelation {
        return this.elementVersions[id]?.relationTo(timestamp) ?: VectorTimestamp.CausalRelation.STRICTLY_BEFORE
    }

    /**
     * Registers a local event (increase own time by 1)
     */
    fun registerLocalEvent() {
        synchronized(this) {
            this.clock[this.processorId] = this.clock[this.processorId] + 1
        }
    }

    /**
     * Marks the Element with the given [id] as updated, i.e. sets its vector time to the current maximum known to this
     * VersionManager.
     *
     * This is to be used when executing local modifications - the state of the updated element is then causal to the
     */
    fun markElementUpdated(id: String): VectorTimestamp {
        synchronized(this) {
            this.elementVersions[id] = this.clock.copy()

            return this.elementVersions[id]!!
        }
    }

    /**
     * @return Whether the version has been updated (the given [timestamp] is newer than or parallel with the timestamp
     *         stored as the version of the element) or not
     */
    fun updateVersion(id: String, timestamp: VectorTimestamp): Boolean {
        synchronized(this) {
            if (this.elementVersions[id]?.let { timestamp.relationTo(it) == VectorTimestamp.CausalRelation.STRICTLY_BEFORE } != true) {
                for ((source, time) in timestamp) {
                    if (this.clock[source] < time) {
                        if (source == this.processorId) {
                            throw IllegalStateException("Received a timestamp with bigger time for own processor ID ${this.processorId}. " +
                                    "This means there must be an inconsistency somewhere.")
                        }

                        this.clock[source] = time
                    }
                }

                this.elementVersions[id] = timestamp

                return true
            }

            return false
        }
    }

    fun onRemove(id: String) {
        this.elementVersions.remove(id)
    }
}
