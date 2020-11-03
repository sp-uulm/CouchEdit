package de.uulm.se.couchedit.processing.common.modbus

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.PublishSubject
import java.util.*

/**
 * Caches the current latest state of all Elements that were exchanged via a [ModificationBusManager].
 *
 * This serves as a central store from which newly attached Processors will get the
 */
@ProcessorScoped
open class ModificationBusStateCacheImpl @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator
) : ModificationBusStateCache {
    private val subject = PublishSubject.create<TimedDiffCollection>().toSerialized()

    private val toAttach = LinkedList<Pair<ModificationPort, Observable<TimedDiffCollection>>>()

    private var isBusy = false

    override fun connect(input: Flowable<TimedDiffCollection>, output: Observer<TimedDiffCollection>) {
        this.subject.subscribe(output)
        input.subscribe { this.process(it) }
    }

    override fun attach(modificationPort: ModificationPort, observable: Observable<TimedDiffCollection>) {
        toAttach.add(Pair(modificationPort, observable))

        if (!isBusy) {
            this.executeAttachment()
        }
    }

    override fun dump(): TimedDiffCollection {
        synchronized(this) {
            return this.modelRepository.dump()
        }
    }

    private fun process(diffs: TimedDiffCollection) {
        synchronized(this) {
            if (toAttach.isNotEmpty()) {
                executeAttachment()
            }

            isBusy = true

            onBeforeProcess(diffs)

            this.applicator.apply(diffs, Applicator.ParallelStrategy.OVERWRITE)

            isBusy = false

            if (toAttach.isNotEmpty()) {
                executeAttachment()
            }

            this.subject.onNext(diffs)
        }
    }

    protected open fun onBeforeProcess(diffs: TimedDiffCollection) {

    }

    protected open fun getCacheContents(): TimedDiffCollection {
        return modelRepository.dump()
    }

    private fun executeAttachment() {
        synchronized(this) {
            while (toAttach.isNotEmpty()) {
                val (port, observable) = toAttach.pop()

                port.connectInputTo(observable.startWith(getCacheContents().filterByElementTypes(port.consumes())))
            }
        }
    }
}
