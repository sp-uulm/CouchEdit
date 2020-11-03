package de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual

import de.uulm.se.couchedit.client.style.InPlaceEditStyleSheet
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import tornadofx.*

/**
 * Visual representing an editable Label.
 *
 * Internally, this is implemented with two JavaFX controls, namely one static [Label] and one dynamic [TextArea].
 *
 * Based on whether the "Edit mode" (as represented by [isEditing]) is active, the dynamic text field is shown or not.
 *
 * The Label stores its currently valid (commited) text in the [textProperty] and [text] variable.
 * The committed text is updated externally (which will overwrite the text both of the dynamic and the static
 * text field).
 *
 * The [startEditing] method enters the UI text edit mode.
 *
 * The [stopEditing] method leaves the edit mode and returns the text entered by the user; the caller must then decide
 * what to do with it (in most cases, set the [text] value to the value returned by [stopEditing]).
 *
 * The [inEditTextProperty] can be used to view the text as the user is updating it.
 */
class LabelVisual(initialText: String = "") : StackPane(), BaseVisual {
    /**
     * Property representing the current, committed & set text of the label
     */
    val textProperty = SimpleStringProperty(initialText)

    /**
     * Convenience Accessor for the current, committed & set text of the label (same as in the textProperty)
     */
    var text: String by PropertyDelegate(textProperty)

    /**
     * Current text property that gets updated whenever the Label is in edit mode and the user is currently updating it
     */
    val inEditTextProperty = SimpleStringProperty(initialText)

    var isEditing = false
        private set

    private val staticLabel = Label(initialText)
    private val textEditLabel = TextArea(initialText)

    init {
        this.children.add(this.staticLabel)
        this.children.add(this.textEditLabel)

        this.textEditLabel.addClass(InPlaceEditStyleSheet.inPlaceEdit)
        this.staticLabel.addClass(InPlaceEditStyleSheet.inPlaceEdit)

        registerTextPropertyUpdateListener()
        registerTextAreaFitContentListener()

        // set label to "not editing" initially
        doStopEditing()
    }

    /**
     * Registers a listener that ensures the [textEditLabel]'s height is adjusted correctly to reflect the required
     * height for the text.
     */
    private fun registerTextAreaFitContentListener() {
        this.textEditLabel.textProperty().addListener { _, _, newVal ->
            adjustTextAreaHeightFitContent()
        }
    }

    /**
     * Registers a listener that ensures that the [textEditLabel] and the [staticLabel] are updated whenever the text
     * in the [textProperty] changes. In addition to that, if the text is changed externally while the editing is active,
     * the text in the [textEditLabel] will be selected.
     */
    private fun registerTextPropertyUpdateListener() {
        this.textProperty.addListener { _, _, newValue ->
            this.textEditLabel.text = newValue
            this.staticLabel.text = newValue

            if (isEditing) {
                this.textEditLabel.selectAll()
            }
        }
    }

    /**
     * Enters the editing mode. This means:
     * * The editable [TextArea] becomes visible
     * * The static [Label] is hidden
     * * The [inEditTextProperty] is setup so that it reflects the currently entered text in the [TextArea]
     * * The [isEditing] flag is set
     */
    fun startEditing() {
        adjustTextAreaHeightFitContent()

        this.staticLabel.isVisible = false
        this.textEditLabel.isVisible = true

        this.textEditLabel.toFront()

        this.textEditLabel.selectAll()
        this.textEditLabel.requestFocus()

        inEditTextProperty.bindBidirectional(textEditLabel.textProperty())
        this.isEditing = true
    }

    fun moveCaretToEnd() {
        this.textEditLabel.apply {
            positionCaret(text.length)
        }
    }

    /**
     * Exits the editing mode (if it is currently active)
     *
     * @return Resulting text from the edit operation
     */
    fun stopEditing(): String {
        if (!this.isEditing) {
            return this.text
        }

        val result = this.textEditLabel.text

        this.doStopEditing()

        return result
    }

    /**
     * Sets the state of the control to the "not editing" mode.
     */
    private fun doStopEditing() {
        textEditLabel.isVisible = false
        staticLabel.isVisible = true

        isEditing = false

        inEditTextProperty.set(this.text)

        staticLabel.toFront()

        this.requestFocus()
    }

    /**
     * Adjusts the height of the editable [TextArea] to the height of the text that is currently entered in it.
     */
    private fun adjustTextAreaHeightFitContent() {
        textEditLabel.apply {
            (this.lookup(".text") as? Text)?.let {
                // TODO: Calculate real margins dynamically?
                this.maxHeight = it.boundsInLocal.height + 5
                requestLayout()
            }
        }
    }

    override fun addChild(visual: Node, index: Int?) {
        index?.let { this.children.add(it + 2, visual) } ?: this.children.add(visual)
    }

    override fun removeChild(visual: Node) {
        this.children.remove(visual)
    }
}
