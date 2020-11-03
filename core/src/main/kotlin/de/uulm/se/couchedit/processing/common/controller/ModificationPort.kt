package de.uulm.se.couchedit.processing.common.controller

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.processing.common.model.diffcollection.DiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.Observable

/**
 * A ModificationPort is the interface through which different application components interact with each other.
 *
 * A component will receive a RxJava [Observable] as its input through the method [connectInputTo].
 * This [Observable] will contain all add, remove or modify operations on the elements specified by [consumes] that occur
 * in the application.
 * In turn, the component will output the results generated by these inputs over the observable which will be returned
 * by the [getOutput] method.
 */
interface ModificationPort {
    val id: String

    /**
     * @return A (OR) List of Element types that this Port can accept as input and deliver sensible output.
     *         The port will receive all elements whose class is assignable to one of the given input classes.
     */
    fun consumes(): List<Class<out Element>>

    /**
     * Receives a [Observable] which the component behind this port should listen on in order to receive updates on its
     * consumed object classes.
     */
    fun connectInputTo(diffPublisher: Observable<TimedDiffCollection>)

    /**
     * Returns an [Observable] on which the [DiffCollection]s will be output that represent the differences occurring
     * in the component behind the Port.
     */
    fun getOutput(): Observable<TimedDiffCollection>
}
