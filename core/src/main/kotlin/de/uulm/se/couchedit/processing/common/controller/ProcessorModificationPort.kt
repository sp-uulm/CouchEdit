package de.uulm.se.couchedit.processing.common.controller

import com.google.inject.name.Named
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.threadly.concurrent.wrapper.limiter.ExecutorLimiter
import java.util.concurrent.ExecutorService

/**
 * Decorator for a [Processor] to use it as a ModificationPort.
 */
abstract class ProcessorModificationPort(
        protected val processor: Processor,
        protected val diffCollectionFactory: DiffCollectionFactory,
        @Named("centralExecutor") protected val centralExecutor: ExecutorService
) : ModificationPort {
    override val id: String = processor.id

    private val inputExecutor = ExecutorLimiter(centralExecutor, processor.maxThreadNumber())

    private var nextDiffCollection = diffCollectionFactory.createMutableTimedDiffCollection()

    private val subject = PublishSubject.create<TimedDiffCollection>().toSerialized()

    /**
     * Flag indicating whether this Processor currently has a task enqueued to the [inputExecutor]
     */
    protected var hasEnqueuedTask = false

    override fun consumes(): List<Class<out Element>> = this.processor.consumes()

    override fun connectInputTo(diffPublisher: Observable<TimedDiffCollection>) {
        diffPublisher.observeOn(Schedulers.computation()).subscribe {
            synchronized(this) {
                this.onIncomingDiffCollection(it)

                if (it.isEmpty()) {
                    return@subscribe
                }

                this.nextDiffCollection.mergeNewerFrom(it)

                if (!this.hasEnqueuedTask) {
                    this.inputExecutor.submit {
                        flush()
                    }
                }

                this.hasEnqueuedTask = true
            }
        }
    }

    override fun getOutput(): Observable<TimedDiffCollection> {
        // like a snake that bites its own tail... Pipe the processed values right back into the upstream subject.

        return this.subject.filter(TimedDiffCollection::isNotEmpty).hide()
    }

    /**
     * Hook for when a DiffCollection comes in.
     * This is executed synchronized on `this`.
     */
    protected open fun onIncomingDiffCollection(diffCollection: TimedDiffCollection) {}

    private fun flush() {
        try {
            doFlush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the actual "flushing" procedure. By default, this has to contain:
     * * Swap out the [nextDiffCollection] with an empty one (synchronized on [nextDiffCollection]!)
     * * Call the [Processor.process] with this DiffCollection value
     * * If the processing result is non-empty, pass it to the [subject]
     */
    abstract fun doFlush()

    /**
     * Gets the [TimedDiffCollection] currently collected by this ModificationPort, then resets it to an empty
     * DiffCollection and returns the previous value.
     */
    protected fun getAndResetNextDiffs(): TimedDiffCollection {
        synchronized(this) {
            this.hasEnqueuedTask = false

            val diffCollection = this.nextDiffCollection

            this.nextDiffCollection = diffCollectionFactory.createMutableTimedDiffCollection()

            return diffCollection
        }
    }

    /**
     * Publishes a [result] on this Port's output into the rest of the system
     */
    protected fun publishResult(result: TimedDiffCollection) {
        this.subject.onNext(result)
    }

    /**
     * Internal access to the processor of the modification port. For debug purposes only.
     */
    val debugProcessor: Processor
        get() = this.processor
}
