package de.uulm.se.couchedit.client.controller.canvas.gef.policy

import de.uulm.se.couchedit.client.controller.canvas.gef.operation.BendStagingContentOperation
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseBendablePart
import org.eclipse.gef.mvc.fx.operations.AbstractCompositeOperation
import org.eclipse.gef.mvc.fx.operations.ForwardUndoCompositeOperation
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation
import org.eclipse.gef.mvc.fx.policies.BendConnectionPolicy
import java.lang.reflect.Field

/**
 * [BendConnectionPolicy] which disables GEF's built in autoconnect (as this interferes with the connection logic)
 */
class NoAutoconnectBendConnectionPolicy : BendConnectionPolicy() {
    override fun canConnect(explicitAnchorIndex: Int) = false

    override fun locallyExecuteOperation() {
        if (this.isInitialized && !isUsePreMoveHints()) {
            /*
             * HACK:
             *
             * OK, things get really messed up here. This is another workaround for GEF not being prepared to handle
             * events while an operation is still in progress.
             *
             * The clean way would be to have a separate executeOperation method for stagingContent,
             * but that would require directly modifying the GEF code (BendConnectionPolicy.move() uses a ton of private
             * variables to do its job). Forking GEF and adding the staging content to its policies would be the far
             * cleaner solution.
             *
             * As it stands now, the implementation details of BendConnectionPolicy are exploited:
             * * BendConnectionPolicy.move() triggers locallyExecuteOperation twice per move event, once for the original
             *   position of the anchors (BendConnectionPolicy:710) and once for the target position
             *   (BendConnectionPolicy:748).
             * * If we "abuse" setVisualBendPoints for publishing in-progress move operations to the rest of the system,
             *   this causes the point being set to its original position, then the position it has been dragged to
             *   for each movement. That again causes many unnecessary (and incorrect) operations being triggered in the
             *   back-end.
             * * Looking at the source code of BendConnectionPolicy, it sets the private variable "usePreMoveHints"
             *   around the call to locallyExecuteOperation when it does use the original coordinates.
             * * We exploit this behavior by doing a staging content update only if usePreMoveHints is false. This way,
             *   we only get the "real" movement coordinates.
             * * Even worse, as usePreMoveHints is private, we need to use reflection for that.
             */
            this.getBendStagingContentOperation()?.apply {
                finalBendPoints = bendOperation.finalBendPoints

                execute(null, null)
            }

        }

        super.locallyExecuteOperation()
    }

    override fun createOperation(): ITransactionalOperation {
        val fwdOp = super.createOperation() as ForwardUndoCompositeOperation

        (host as? BaseBendablePart<*, *>)?.let { fwdOp.add(BendStagingContentOperation(it)) }

        return fwdOp
    }

    fun getBendStagingContentOperation(): BendStagingContentOperation? {
        return (super.getOperation() as? AbstractCompositeOperation)?.let {
            (it.operations[1] as? BendStagingContentOperation)
                    ?: it.operations.filterIsInstance(BendStagingContentOperation::class.java).firstOrNull()
        }
    }

    private fun isUsePreMoveHints() = usePreMoveHintsField.getBoolean(this)

    companion object {
        /**
         * Ugly hack
         */
        val usePreMoveHintsField: Field by lazy {
            NoAutoconnectBendConnectionPolicy::class.java.superclass.getDeclaredField("usePreMoveHints").apply {
                this.isAccessible = true
            }
        }
    }
}
