package de.uulm.se.couchedit.processing.common.modbus

import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Observer

/**
 * Caches the current latest state of all Elements that were exchanged via a [ModificationBusManager].
 *
 * The ModificationBusStateCache will cache all Elements that are exchanged via the attached ModificationBusManager just
 * like a [de.uulm.se.couchedit.processing.common.controller.Processor] would.
 */
interface ModificationBusStateCache {
    /**
     * Attaches this  [ModificationBusStateCache] to a [ModificationBusManager].
     * Here, the input is the flow of all Elements that are exchanged in this Manager, and the output is the
     * flow of all Elements going out to the attached ModificationPorts.
     */
    fun connect(input: Flowable<TimedDiffCollection>, output: Observer<TimedDiffCollection>)

    /**
     * Attaches a [ModificationPort] to the flow of Elements.
     *
     * The StateCache is supposed to first enqueue its currently cached state, then passthrough the [observable].
     */
    fun attach(modificationPort: ModificationPort, observable: Observable<TimedDiffCollection>)

    /**
     * Returns all values of the current ModelRepository.
     */
    fun dump(): TimedDiffCollection
}
