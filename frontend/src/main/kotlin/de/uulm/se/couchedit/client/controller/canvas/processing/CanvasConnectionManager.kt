package de.uulm.se.couchedit.client.controller.canvas.processing

import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseBendablePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.graphicobject.BendableLinePart
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.subjects.ReplaySubject
import org.eclipse.gef.mvc.fx.policies.ContentPolicy

/**
 * Responsible for connecting / disconnecting connection anchorages when ConnectionEnds get added or removed.
 */
internal class CanvasConnectionManager {
    internal val elementUpdateSubject = ReplaySubject.create<Triple<BendableLinePart, BasePart<*, *>, ModelDiff>>()

    init {
        this.elementUpdateSubject.observeOn(JavaFxScheduler.platform()).subscribe { (linePart, otherPart, diff) ->
            this.onIncomingDiff(linePart, otherPart, diff)
        }
    }

    private fun onIncomingDiff(linePart: BendableLinePart, otherPart: BasePart<*, *>, diff: ModelDiff) {
        val connectionEnd = diff.affected as? ConnectionEnd<*, *> ?: return

        val role = BaseBendablePart.getRole(connectionEnd.isEndConnection)

        val otherContent = otherPart.getContent()

        val contentPolicy = linePart.getAdapter(ContentPolicy::class.java)
        if (diff is ElementAddDiff || diff is ElementModifyDiff) {
            if (connectionEnd.probability != ProbabilityInfo.Explicit) {
                return
            }
            contentPolicy.init()

            contentPolicy.attachToContentAnchorage(otherContent, role)

            contentPolicy.commit()
        } else if (diff is ElementRemoveDiff) {
            if (linePart.contentAnchoragesUnmodifiable.containsEntry(otherContent, role)) {
                contentPolicy.init()

                contentPolicy.attachToContentAnchorage(otherContent, role)

                contentPolicy.commit()
            }
        }
    }
}
