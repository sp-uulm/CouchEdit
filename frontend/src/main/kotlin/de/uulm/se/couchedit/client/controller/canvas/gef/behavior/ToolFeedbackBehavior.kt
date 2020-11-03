package de.uulm.se.couchedit.client.controller.canvas.gef.behavior

import de.uulm.se.couchedit.client.interaction.pattern.InteractionPattern
import de.uulm.se.couchedit.client.util.gef.rootDrawingPart
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import javafx.beans.value.ObservableValue
import org.eclipse.gef.mvc.fx.behaviors.AbstractBehavior
import org.eclipse.gef.mvc.fx.parts.IFeedbackPartFactory
import org.eclipse.gef.mvc.fx.viewer.IViewer

/**
 * @todo provide feedback to the user while an interaction pattern is active.
 */
class ToolFeedbackBehavior : AbstractBehavior() {
    private val viewer by lazy { host.root.viewer }

    private val rootDrawingPart by lazy { viewer.rootDrawingPart }

    private val feedbackPartFactory by lazy { getFeedbackPartFactory(viewer) }

    private val toolModel by lazy {
        viewer.getAdapter(ToolModel::class.java) ?: throw IllegalStateException("No ToolModel bound to viewer!")
    }

    override fun doActivate() {
        toolModel.currentInteractionPatternProperty.addListener(this::onInteractionPatternChange)
    }

    override fun doDeactivate() {
        toolModel.currentInteractionPatternProperty.removeListener(this::onInteractionPatternChange)
    }

    /**
     * Callback for when the current [InteractionPattern] of the [ToolModel] changes.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onInteractionPatternChange(
            observable: ObservableValue<out InteractionPattern?>,
            oldValue: InteractionPattern?,
            newValue: InteractionPattern?) {
        removeFeedback()

        newValue?.let { addFeedback(it) }
    }

    /**
     * GEF expects feedback parts to be created depending on a certain target, and the implementation of
     * [AbstractBehavior.addFeedback]
     */
    private fun addFeedback(interactionPattern: InteractionPattern) {
        val targetList = listOf(rootDrawingPart)
        val targetSet = targetList.toSet()

        check(!hasFeedback(targetSet)) { "There cannot be two feedbacks active at once." }

        val feedbackParts = feedbackPartFactory?.createFeedbackParts(targetList, mapOf(
                InteractionPattern::class.java to interactionPattern
        )) ?: emptyList()

        feedbackPerTargetSet[targetSet] = feedbackParts

        if (feedbackParts.isNotEmpty()) {
            addAnchoreds(targetList, feedbackParts)
        }
    }

    private fun removeFeedback() {
        val targetSet = setOf(rootDrawingPart)

        if(!hasFeedback(targetSet)) {
            return
        }

        removeFeedback(setOf(rootDrawingPart))
    }

    override fun getFeedbackPartFactory(viewer: IViewer?): IFeedbackPartFactory? {
        return super.getFeedbackPartFactory(viewer, ROLE_TOOL_FEEDBACK_PART_FACTORY)
    }

    companion object {
        const val ROLE_TOOL_FEEDBACK_PART_FACTORY = "TOOL_FEEDBACK_PART_FACTORY"
    }
}
