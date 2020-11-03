package de.uulm.se.couchedit.processing.common.controller

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import java.util.*

/**
 * Interface for a class that takes a [DiffCollection] as its input and produces the consequent [DiffCollection]
 * as its output instantly. Simplifies the [ModificationPort] semantic to a single called method.
 */
interface Processor {
    val id
        get() = "processor_${this.javaClass.name}_${UUID.randomUUID()}"

    /**
     * @return A (OR) List of Element types that this Processor must be presented with in order to do its job.
     */
    fun consumes(): List<Class<out Element>>

    /**
     * Processes the given diffs, then returns a set of new diffs resulting from them in form of a [DiffCollection]
     */
    fun process(diffs: TimedDiffCollection): TimedDiffCollection

    /**
     * Number of threads that are concurrently allowed to be run of this Processor.
     *
     * By default, processors are single threaded, implementations must override this function if they can run concurrently
     */
    fun maxThreadNumber(): Int = 1
}
