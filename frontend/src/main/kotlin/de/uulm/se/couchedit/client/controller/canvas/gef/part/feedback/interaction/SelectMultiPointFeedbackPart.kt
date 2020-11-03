package de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback.interaction

import de.uulm.se.couchedit.client.interaction.pattern.SelectMultiPoint
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.LineVisual
import de.uulm.se.couchedit.model.graphic.shapes.Point
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.AbstractFeedbackPart
import org.eclipse.gef.geometry.planar.Point as GEFPoint

/**
 * @todo support feedbacks other than line??
 */
class SelectMultiPointFeedbackPart : AbstractFeedbackPart<Node>() {
    /**
     * InteractionPattern to which this Part should provide feedback.
     * Must be set before adding this FeedbackPart to anything or an exception will occur.
     */
    lateinit var interactionPattern: SelectMultiPoint

    private val currentPoints = mutableListOf<GEFPoint>()

    override fun doActivate() {
        this.interactionPattern.candidatePointObservable.addListener(this::onCandidatePointChange)
        this.interactionPattern.acceptedPointObservable.addListener(this::onAcceptedPointsChange)

        replaceCurrentPoints(this.interactionPattern.acceptedPointObservable)
        refreshVisual()
    }

    override fun doDeactivate() {
        this.interactionPattern.candidatePointObservable.removeListener(this::onCandidatePointChange)
        this.interactionPattern.acceptedPointObservable.removeListener(this::onAcceptedPointsChange)
    }

    override fun doCreateVisual(): Node {
        return LineVisual()
    }

    override fun doRefreshVisual(visual: Node?) {
        val line = visual as? LineVisual ?: throw IllegalArgumentException("Visual must be LineVisual, got $visual")

        val candidate = interactionPattern.candidatePointObservable.value

        val linePoints = if (candidate != null) currentPoints + candidate.toGEF() else currentPoints

        if (linePoints.size >= 2) {
            line.setPoints(linePoints)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCandidatePointChange(
            observable: ObservableValue<out Point?>,
            oldPoint: Point?,
            newPoint: Point?
    ) {
        refreshVisual()
    }

    private fun onAcceptedPointsChange(change: ListChangeListener.Change<out Point>) {
        replaceCurrentPoints(change.list)

        refreshVisual()
    }

    private fun replaceCurrentPoints(newPoints: List<Point>) {
        this.currentPoints.clear()
        this.currentPoints.addAll(newPoints.map { it.toGEF() })
    }

    private fun Point.toGEF(): GEFPoint = GEFPoint(this.x, this.y)
}
