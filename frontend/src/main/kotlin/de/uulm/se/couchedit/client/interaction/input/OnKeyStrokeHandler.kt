package de.uulm.se.couchedit.client.interaction.input

import javafx.scene.input.KeyEvent

/**
 *
 */
interface OnKeyStrokeHandler {
    fun abortPress() {}

    fun finalRelease(e: KeyEvent) {}

    fun initialPress(e: KeyEvent) {}

    fun press(e: KeyEvent) {}

    fun release(e: KeyEvent) {}
}
