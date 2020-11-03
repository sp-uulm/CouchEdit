package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Singleton
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.base.suggestions.BaseSuggestion
import de.uulm.se.couchedit.model.base.suggestions.SuggestionFor
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.composition.relations.ComponentOf
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

/**
 * Implementation of a [ModificationPort] which does not do automated processing, but instead publishes actions made in
 * the front-end to the rest of the application, and applies incoming operations to the concrete visual representation
 * in the editor.
 */
@Singleton
internal class CanvasModificationPort : ModificationPort {
    override val id: String
        get() = "Canvas_${UUID.randomUUID()}"

    private val observers = mutableListOf<Observer<TimedDiffCollection>>()

    private val disposables = mutableMapOf<Observer<TimedDiffCollection>, Disposable>()

    internal val outputSubject = BehaviorSubject.create<TimedDiffCollection>()

    private var input: Observable<TimedDiffCollection>? = null
        set(value) {
            for (observer in this.observers) {
                disposables[observer]?.dispose()

                value?.subscribe(observer)
            }

            field = value
        }

    override fun consumes(): List<Class<out Element>> = listOf(
            GraphicObject::class.java,
            ConnectionEnd::class.java,
            BaseSuggestion::class.java,
            SuggestionFor::class.java,
            ComponentOf::class.java,
            HotSpotDefinition::class.java,
            AttributeBag::class.java,
            AttributesFor::class.java
    )

    override fun getOutput(): Observable<TimedDiffCollection> = this.outputSubject.hide()

    override fun connectInputTo(diffPublisher: Observable<TimedDiffCollection>) {
        this.input = diffPublisher
    }

    internal fun addObserver(observer: Observer<TimedDiffCollection>) {
        this.input?.subscribe(observer)

        this.observers.add(observer)
    }
}
