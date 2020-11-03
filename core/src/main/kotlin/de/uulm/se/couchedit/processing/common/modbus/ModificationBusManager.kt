package de.uulm.se.couchedit.processing.common.modbus

import com.google.inject.Inject
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.BackpressureStrategy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * This class is responsible for coordinating the processing bus.
 *
 * It takes care of publishing relevant [ModelDiff] info to all
 * [ModificationPort]s potentially interested in it.
 */
class ModificationBusManager @Inject constructor(
        initialPorts: Set<ModificationPort>,
        private val modificationBusStateCache: ModificationBusStateCache,
        /**
         * Hook function that gets all [diffCollection]s that go to a certain [modificationPort].
         *
         * This is done before empty DiffCollections are filtered out.
         * Currently used for testing only
         *
         * @todo More generic approach
         */
        private val debugDiffCollectionInterceptor: ((ModificationPort, TimedDiffCollection) -> Unit)? = null
) {
    private val ports = mutableSetOf<ModificationPort>()

    /**
     * Immutable view to the [ModificationPort]s available to this [ModificationBusManager]
     */
    val debugPorts
        get() = this.ports.toList()

    private val diffBusInput: Subject<TimedDiffCollection> = PublishSubject.create<TimedDiffCollection>().toSerialized()

    private val diffBusOutput: Subject<TimedDiffCollection> = PublishSubject.create<TimedDiffCollection>().toSerialized()

    init {
        setupCache()

        initialPorts.forEach(this::registerModificationPort)
    }

    private fun setupCache() {
        this.modificationBusStateCache.connect(
                diffBusInput.toFlowable(BackpressureStrategy.BUFFER),
                diffBusOutput
        )
    }

    /**
     * Registers a new [ModificationPort] to receive all [ModelDiff]s which it has registered interest for in its
     * [ModificationPort.consumes] method.
     * Also sets up the Bus so that all output events of the given [modificationPort] will be published to other
     * interested [ModificationPort]s.
     */
    fun registerModificationPort(modificationPort: ModificationPort) {
        var observable = this.diffBusOutput.observeOn(Schedulers.computation())
                .map { it.filterByElementTypes(modificationPort.consumes()) }

        if (debugDiffCollectionInterceptor != null) {
            observable = observable.doOnNext { debugDiffCollectionInterceptor!!(modificationPort, it) }
        }

        observable = observable.filter(TimedDiffCollection::isNotEmpty)
                .map(TimedDiffCollection::copy)
                .hide()

        modificationPort.getOutput().subscribe(this.diffBusInput)
        this.modificationBusStateCache.attach(modificationPort, observable)

        this.ports.add(modificationPort)
    }

    /**
     * Exports the contents of the [modificationBusStateCache] to a DiffCollection for saving.
     */
    fun exportSystemState(): TimedDiffCollection {
        return this.modificationBusStateCache.dump()
    }
}
