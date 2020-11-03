package de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback.interaction

import de.uulm.se.couchedit.client.interaction.pattern.SelectRectangularRegion
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.RectangleVisual
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.AbstractFeedbackPart

class SelectRectangularRegionFeedbackPart : AbstractFeedbackPart<Node>() {
    /**
     * InteractionPattern to which this Part should provide feedback.
     * Must be set before adding this FeedbackPart to anything or an exception will occur.
     */
    lateinit var interactionPattern: SelectRectangularRegion

    override fun doActivate() {
        this.interactionPattern.candidateRectangleProperty.addListener(this::onCandidateRectangleChange)
    }

    override fun doCreateVisual(): Node {
        return RectangleVisual()
    }

    override fun doRefreshVisual(visual: Node?) {
        check(visual is RectangleVisual) { "Visual must be RectangleVisual, got $visual" }

        with(interactionPattern.candidateRectangleProperty.value ?: return) {
            visual.translateX = x
            visual.translateY = y

            visual.setPrefSize(w, h)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCandidateRectangleChange(
            observable: ObservableValue<out Rectangular?>,
            oldRectangle: Rectangular?,
            newPoint: Rectangular?
    ) {
        refreshVisual()
    }
}
