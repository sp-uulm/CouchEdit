package de.uulm.se.couchedit.systemtestutils.controller.manager

import de.uulm.se.couchedit.processing.common.controller.ProcessorModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.systemtestutils.controller.processing.TestProcessorModificationPort
import de.uulm.se.couchedit.systemtestutils.controller.processing.model.TrackableDiffCollectionWrapper
import java.util.concurrent.CompletableFuture

/**
 * Registry for TestModificationPorts so that test scenarios can access their status.
 *
 * This is needed due to the asynchronous, parallel nature of the system - it would otherwise be impossible to detect
 * when the system has finished processing and results (including the processing time) can be used.
 *
 * This works in conjunction with the [TestProcessorModificationPort] and the [TrackableDiffCollectionWrapper].
 *
 * The rough procedure is as follows:
 * 1. When starting processing, the test case registers a [CompletableFuture] to be finished when the system processing
 *    finishes. It does this via the [setCompletableFuture] method, also giving the ID of the
 *    [TrackableDiffCollectionWrapper] that has to be processed including all of its descendents before the system
 *    may be considered "finished".
 * 2. The processing may now start. The Registry has to be informed about all DiffCollections that are being consumed
 *    and output in the system:
 *    * When pushing a DiffCollection to a port, the ModificationBusManager calls [onDiffCollectionIncoming], giving the
 *      ID of the Port and the [TrackableDiffCollectionWrapper]. This is only important if the DiffCollection has been
 *      filtered to an empty Collection (which will not be passed to the Port at all).
 *      The Registry will then store that that port has received and "processed" the DiffCollection
 *    * Otherwise, the Port receives the DiffCollection as usual.
 *
 *      It is obliged to store which DiffCollections it has received
 *      (as ModificationPorts may merge multiple DiffCollections into one, all of the IDs must be kept).
 *
 *      After the Processor has finished its current Processing run, it must call [onProcessingFinish] with its ID,
 *      the DiffCollection IDs that were handled in this processing and the new DiffCollection which was generated.
 *      This causes the Registry to now also expect confirmation of that DiffCollection by all other Processors.
 * 3. After all DiffCollections have been processed by this Processor, the given future is completed with the processing
 *    times taken up by all Processors.
 *
 * @see [TestProcessorModificationPort]
 */
class TestModificationPortRegistry {
    private val ports = mutableListOf<ProcessorModificationPort>()

    private val unprocessedDiffCollections = mutableMapOf<String, MutableSet<String>>()

    /**
     * Adds the given [future] to be completed after all processors are ready.
     *
     * The value then contained in the future is a map from Processor IDs to the time spent in each Processor.
     */
    private var future: CompletableFuture<Map<String, Double>>? = null

    /**
     * Counter for the IDs given to the DiffCollectionWrappers.
     */
    private var diffCollectionIdCounter: Int = 0


    fun registerPort(port: ProcessorModificationPort) {
        synchronized(unprocessedDiffCollections) {
            this.unprocessedDiffCollections[port.id] = mutableSetOf()

            this.ports.add(port)
        }
    }

    /**
     * Configures this Registry so that the [future] will be completed with a mapping of ModificationPort IDs to the
     * processing time spent in this processor after the next burst of data is completely processed.
     *
     * This also resets the ready statuses in this registry, i.e. a new processing cycle will likely need to be started
     * for the future to complete
     *
     * @param future The Future to be completed with a map of ModificationPort ID to processing time after all
     *               DiffCollections have been received
     * @param initialDiffCollectionIds The DiffCollections (and their descendents) that have to be finished before the
     *                                 [future] can be completed
     */
    fun setCompletableFuture(future: CompletableFuture<Map<String, Double>>, initialDiffCollectionIds: Set<String>) {
        synchronized(unprocessedDiffCollections) {
            this.future = future

            for (key in unprocessedDiffCollections.keys) {
                unprocessedDiffCollections[key] = initialDiffCollectionIds.toMutableSet()
            }
        }

    }

    /**
     * Callback for when a ModificationPort receives a DiffCollection from the ModificationBusManager.
     * This is needed because ModificationPorts won't get (filtered-to-)empty DiffCollections and so they won't
     * acknowledge their processing in [onProcessingFinish], either.
     */
    fun onDiffCollectionIncoming(id: String, diffCollection: DiffCollection) {
        synchronized(unprocessedDiffCollections) {
            /*
             * If an empty DiffCollection is received, acknowledge its receival instantly
             */
            if (diffCollection.isEmpty() && diffCollection is TrackableDiffCollectionWrapper) {
                val waitingForDiffCollections = unprocessedDiffCollections[id] ?: return

                waitingForDiffCollections.removeAll(diffCollection.ids)

                //printWaiting("$id has received [${diffCollection.ids.joinToString(",")}] as empty")

                if (waitingForDiffCollections.isEmpty()) {
                    checkIfComplete()
                }
            }
        }
    }

    /**
     * Callback for when the ModificationPort with the given [id] finishes its processing run.
     * The [TestProcessorModificationPort] will have to store the DiffCollectionWrapper IDs that it has accumulated
     * and give them as the [handledCollections] parameter after processing is finished.
     *
     * @param id The identifier of the ModificationPort reporting its processing finish
     * @param handledCollections The DiffCollection IDs that have been handled in this processing run
     * @param output The new DiffCollection that resulted from the processing.
     */
    fun onProcessingFinish(id: String, handledCollections: Set<String>, output: DiffCollection?) {
        synchronized(unprocessedDiffCollections) {
            val unprocessedForId = unprocessedDiffCollections[id]
                    ?: throw IllegalArgumentException("Processor $id not registered")

            for (collectionId in handledCollections) {
                unprocessedForId.remove(collectionId)
            }

            /*
            printWaiting("$id has finished processing " + handledCollections.joinToString(";", "[", "]") +
                    if (output.isNullOrEmpty()) " and produced empty output" else
                        " Newly produced:  ${(output as? TrackableDiffCollectionWrapper)?.ids}")
            */
            if (output is TrackableDiffCollectionWrapper && output.isNotEmpty()) {
                unprocessedDiffCollections.forEach { (_, unprocessedForPort) ->
                    unprocessedForPort.addAll(output.ids)
                }
            } else {
                checkIfComplete()
            }
        }
    }

    /**
     * Returns an Identifier for the next DiffCollectionWrapper that is to be produced
     */
    fun getDiffCollectionId(): String {
        val id = synchronized(diffCollectionIdCounter) {
            diffCollectionIdCounter += 1

            diffCollectionIdCounter
        }

        return "DiffCollection" + id.toString()
    }

    private fun checkIfComplete() {
        synchronized(unprocessedDiffCollections) {
            if (unprocessedDiffCollections.values.all { it.isEmpty() }) {
                completeFuture()
            }
        }
    }

    private fun completeFuture() {
        val result = mutableMapOf<String, Double>()

        for (port in this.ports) {
            if (port is TestProcessorModificationPort) {
                result[port.id] = port.getAndResetTime()
            }
        }

        //printWaiting("FINISH")

        future?.complete(result)
    }

    private fun printWaiting(reason: String) {
        val waiting = unprocessedDiffCollections.map { (k, v) ->
            k + ": " + v.joinToString(";", "[", "]")
        }

        println("TMPR: $reason - Still waiting for:" + waiting.joinToString(" / "))
    }
}
