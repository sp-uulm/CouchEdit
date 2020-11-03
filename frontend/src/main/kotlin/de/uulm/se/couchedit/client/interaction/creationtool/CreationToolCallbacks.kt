package de.uulm.se.couchedit.client.interaction.creationtool

import de.uulm.se.couchedit.model.base.Element

/**
 * Callbacks that the creation tool has to its host
 */
interface CreationToolCallbacks {
    /**
     * To be called after the tool has finished its interactions and returns a set of new Elements to be inserted
     * into the application data model.
     */
    fun finishCreation(toInsert: Set<Element>)

    /**
     * Callback after the tool interaction has been aborted.
     */
    fun abortCreation()
}
