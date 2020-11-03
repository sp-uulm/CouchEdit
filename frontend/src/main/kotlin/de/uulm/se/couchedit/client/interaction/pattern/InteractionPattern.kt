package de.uulm.se.couchedit.client.interaction.pattern

/**
 * (Marker) interface for all interaction patterns.
 *
 * An interaction pattern describes a certain style of interacting with the canvas for the user.
 * To this end, the active tool should pass through all incoming input (mouse and keyboard) events to the interaction
 * pattern, which then reacts to the inputs.
 *
 * The pattern may then use [InteractionPatternCallbacks] to pass its results back to the original Tool instance.
 *
 * The definitions of InteractionPatterns are also used by the InteractionFeedbackFactory to create fitting feedback
 * images.
 */
interface InteractionPattern
