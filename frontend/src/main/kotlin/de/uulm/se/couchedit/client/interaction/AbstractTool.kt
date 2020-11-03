package de.uulm.se.couchedit.client.interaction

import de.uulm.se.couchedit.client.interaction.input.*
import de.uulm.se.couchedit.client.interaction.pattern.InteractionPattern
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import tornadofx.*

/**
 * Abstract base class providing common [Tool] functionality.
 *
 * Operation of such a tool consists of multiple different [InteractionPattern]s, representing the steps that the
 * user must execute to successfully use the tool's functionality.
 */
abstract class AbstractTool : Tool {
    /**
     * Property containing the currently active [InteractionPattern] of this tool.
     */
    final override val currentInteractionPatternProperty: ObjectProperty<InteractionPattern?> = SimpleObjectProperty(null)

    /**
     * Access to the currently active interaction pattern of the Tool through a "normal" variable.
     */
    var currentInteractionPattern: InteractionPattern? by currentInteractionPatternProperty

    /**
     * Internal Property (r/w) indicating whether this Tool is currently active
     */
    private val activePropertyInternal: ReadOnlyBooleanWrapper = ReadOnlyBooleanWrapper(false)

    /**
     * Property indicating whether this Tool is currently active
     */
    override val activeProperty: ReadOnlyBooleanProperty = activePropertyInternal.readOnlyProperty

    /**
     * Access to information as to whether the tool is currently active through a "normal" variable.
     */
    final override var isActive: Boolean by activePropertyInternal
        private set

    /**
     * Sets the current status of this tool to "active", i.e. it is currently accepting input from the user. Usually,
     * this also means that the tool has a [currentInteractionPattern].
     *
     * As the interaction used to activate the tool is different for each [Tool] implementation, this method is only for
     * use by subclasses; the interaction cannot be started from the outside.
     */
    protected fun activate() {
        isActive = true

        this.onActivate()
    }

    /**
     * Sets the current status of the tool to "inactive", which usually means that all previously input data is cleared.
     * The user may choose to restart the interaction after the deactivation.
     */
    override fun deactivate() {
        isActive = false
        this.currentInteractionPattern = null

        this.onDeactivate()
    }

    /**
     * Hook that is called after the [isActive] status of the tool has been enabled.
     */
    protected open fun onActivate() {}

    /**
     * Hook that is called after the [isActive] status of the tool has been disabled.
     */
    protected open fun onDeactivate() {}

    /*
     * Handler callbacks. The default behavior is to pass occurring events to the active InteractionPattern.
     * Subclasses must override these methods to be able to activate themselves, but they usually need to call super
     * to have the InteractionPatterns work correctly.
     */

    /* ---------- OnClickHandler callbacks -------------- */
    override fun onClick(e: CanvasMouseEvent) {
        (currentInteractionPattern as? OnClickHandler)?.onClick(e)
    }

    /* ---------- OnHoverHandler callbacks -------------- */
    override fun onHover(e: CanvasMouseEvent) {
        (currentInteractionPattern as? OnHoverHandler)?.onHover(e)
    }

    /* ---------- OnDragHandler callbacks -------------- */
    override fun startDrag(e: CanvasMouseEvent) {
        (currentInteractionPattern as? OnDragHandler)?.startDrag(e)
    }

    override fun endDrag(e: CanvasMouseEvent) {
        (currentInteractionPattern as? OnDragHandler)?.endDrag(e)
    }

    override fun abortDrag() {
        (currentInteractionPattern as? OnDragHandler)?.abortDrag()
    }

    override fun drag(e: CanvasMouseEvent) {
        (currentInteractionPattern as? OnDragHandler)?.drag(e)
    }

    /* ---------- OnKeyStrokeHandler callbacks -------------- */
    override fun abortPress() {
        (currentInteractionPattern as? OnKeyStrokeHandler)?.abortPress()
    }

    override fun finalRelease(e: KeyEvent) {
        (currentInteractionPattern as? OnKeyStrokeHandler)?.finalRelease(e)
    }

    override fun initialPress(e: KeyEvent) {
        (currentInteractionPattern as? OnKeyStrokeHandler)?.initialPress(e)
    }

    override fun press(e: KeyEvent) {
        (currentInteractionPattern as? OnKeyStrokeHandler)?.press(e)
    }

    override fun release(e: KeyEvent) {
        (currentInteractionPattern as? OnKeyStrokeHandler)?.release(e)
    }

    /**
     * Hook for when a key is released while this tool is the current tool of the application
     */
    fun onKeyUp(e: KeyEvent) {
        if (e.code == KeyCode.ESCAPE) {
            this.deactivate()
        }
    }
}
