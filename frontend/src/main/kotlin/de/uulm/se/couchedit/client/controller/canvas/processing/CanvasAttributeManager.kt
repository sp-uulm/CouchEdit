package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Inject
import com.google.inject.Singleton
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository

@ProcessorScoped
class CanvasAttributeManager @Inject constructor(private val queries: GraphicObjectQueries) {
    private val attributeChangeListeners = mutableMapOf<String, (String, Set<Pair<ElementReference<AttributeBag>, AttributeBag?>>) -> Unit>()

    /**
     * Adds a function that gets called every time the Attributes of an element change.
     * The first parameter of the [listener] is the ID of the Element for which the Attributes have changed,
     * the second one is the Set of [AttributeBag]s that have changed for this Element, as a pair of their reference
     * and their new state (potentially null if the Bag has been deleted).
     */
    fun addAttributeChangeListener(key: String, listener: (String, Set<Pair<ElementReference<AttributeBag>, AttributeBag?>>) -> Unit) {
        this.attributeChangeListeners[key] = listener
    }

    /**
     * Removes the attribute change listener which has been previously added by [addAttributeChangeListener] with the
     * given [key].
     */
    fun removeAttributeChangeListener(key: String) {
        this.attributeChangeListeners.remove(key)
    }

    fun onAttributesChanged(graphicObjectId: String, newBags: Set<Pair<ElementReference<AttributeBag>, AttributeBag?>>) {
        for(listener in this.attributeChangeListeners.values) {
            listener(graphicObjectId, newBags)
        }
    }

    fun getBagsForElement(id: String): Map<Class<out AttributeBag>, MutableSet<AttributeBag>> {
        val attributeBags = queries.getAttributeBagsFor(id)

        val ret = mutableMapOf<Class<out AttributeBag>, MutableSet<AttributeBag>>()

        for(bag in attributeBags) {
            val bagSet = ret.getOrPut(bag::class.java) {
                mutableSetOf()
            }

            bagSet.add(bag)
        }

        return ret
    }
}
