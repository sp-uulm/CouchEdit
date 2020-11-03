package de.uulm.se.couchedit.client.interaction.creationtool

import de.uulm.se.couchedit.client.interaction.AbstractTool
import de.uulm.se.couchedit.model.base.Element

/**
 * Base class for Tools that can be used to create one or more [Element]s during their interactions.
 */
abstract class CreationTool : AbstractTool() {
    /**
     * Callbacks that are used by the tool for passing the resulting Elements back to the application when an interaction
     * completes successfully.
     */
    var callbacks: CreationToolCallbacks? = null

    override fun onActivate() {
        if (callbacks == null) {
            throw IllegalStateException("Callbacks must not be null at the point where this tool is activated")
        }
    }

    protected fun finishInteraction(elements: Set<Element>) {
        check(callbacks != null) { "Callbacks must not be null on interaction finish!" }

        callbacks?.finishCreation(elements)

        this.deactivate()
    }

    /**
     * To be used by subclasses of the CreationTool when the user executes an action that causes the interaction to be
     * aborted.
     */
    protected fun abortInteraction() {
        this.deactivate()
    }
}
