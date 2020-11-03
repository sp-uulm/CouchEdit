package de.uulm.se.couchedit.client.interaction.pattern

/**
 * Callbacks that the [InteractionPattern] may use to pass its collected information back to the originating Tool.
 */
interface InteractionPatternCallbacks<T> {
    /**
     * Should be called when the user has executed actions with the tool that generate data and has confirmed this
     * action, so that the data is now able to be used with the tool.
     */
    fun onResult(value: T)

    fun onAbort()
}
