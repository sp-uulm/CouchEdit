package de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject

import de.uulm.se.couchedit.client.controller.canvas.gef.part.TextEditModePart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.LabelVisual
import de.uulm.se.couchedit.model.graphic.shapes.Label
import javafx.beans.property.SimpleStringProperty

class LabelPart : BaseRectangularPart<LabelVisual, Label>(), TextEditModePart<LabelVisual> {
    override var text: String
        get() = this.content?.shape?.text ?: ""
        set(value) {
            this.content?.shape?.apply {
                this.text = value
            }

            this.stagingContent?.shape?.apply {
                this.text = value
            }

            this.publishContent(false)
            this.refreshVisual()
        }

    override var stagingTextProperty = SimpleStringProperty(text)
        private set

    override fun doCreateVisual(): LabelVisual? {
        val visual = LabelVisual()

        this.stagingTextProperty = visual.inEditTextProperty

        return visual
    }

    override fun doRefreshVisual(visual: LabelVisual?) {
        super.doRefreshVisual(visual)

        visual?.apply { text = content?.shape?.text ?: "" }
    }

    override fun startEditing() {
        this.visual?.startEditing()
    }

    override fun stopEditing(): String {
        return this.visual?.stopEditing() ?: ""
    }

    override fun isEditing(): Boolean = this.visual?.isEditing == true

    override fun caretToEnd() {
        this.visual?.moveCaretToEnd()
    }
}
