package de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback

import com.google.inject.Inject
import com.google.inject.Injector
import de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback.interaction.SelectMultiPointFeedbackPart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback.interaction.SelectRectangularRegionFeedbackPart
import de.uulm.se.couchedit.client.interaction.pattern.InteractionPattern
import de.uulm.se.couchedit.client.interaction.pattern.SelectMultiPoint
import de.uulm.se.couchedit.client.interaction.pattern.SelectRectangularRegion
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.IFeedbackPart
import org.eclipse.gef.mvc.fx.parts.IFeedbackPartFactory
import org.eclipse.gef.mvc.fx.parts.IVisualPart

class FeedbackPartFactory @Inject constructor(private val injector: Injector) : IFeedbackPartFactory {
    override fun createFeedbackParts(
            targets: List<IVisualPart<out Node>>,
            contextMap: MutableMap<Any, Any>): List<IFeedbackPart<out Node>>? {
        val pattern = contextMap[InteractionPattern::class.java] ?: return null

        if (pattern is SelectMultiPoint) {
            val feedbackPart = injector.getInstance(SelectMultiPointFeedbackPart::class.java)

            feedbackPart.interactionPattern = pattern

            return listOf(feedbackPart)
        }

        if (pattern is SelectRectangularRegion) {
            val feedbackPart = injector.getInstance(SelectRectangularRegionFeedbackPart::class.java)

            feedbackPart.interactionPattern = pattern

            return listOf(feedbackPart)
        }

        return null
    }
}
