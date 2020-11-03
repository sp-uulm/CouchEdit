package de.uulm.se.couchedit.client.controller.canvas.gef.operation

import de.uulm.se.couchedit.client.controller.canvas.gef.part.BaseBendablePart
import org.eclipse.core.commands.operations.AbstractOperation
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation
import org.eclipse.gef.mvc.fx.parts.IBendableContentPart

/**
 * Operation to bend the "staging" content of a bendable part.
 */
class BendStagingContentOperation(private val part: BaseBendablePart<*, *>) : AbstractOperation("BendStagingContent"), ITransactionalOperation {
    val initialBendPoints = mutableListOf<IBendableContentPart.BendPoint>()
    var finalBendPoints = mutableListOf<IBendableContentPart.BendPoint>()
        set(value) {
            field.clear()
            field.addAll(value)
        }

    init {
        initialBendPoints.addAll(part.getStagingContentBendPoints())
        finalBendPoints.addAll(part.getStagingContentBendPoints())
    }

    override fun execute(p0: IProgressMonitor?, p1: IAdaptable?): IStatus {
        part.setStagingContentBendPoints(finalBendPoints)
        return Status.OK_STATUS
    }

    override fun redo(p0: IProgressMonitor?, p1: IAdaptable?): IStatus = this.execute(p0, p1)

    override fun undo(p0: IProgressMonitor?, p1: IAdaptable?): IStatus {
        part.setStagingContentBendPoints(initialBendPoints)
        return Status.OK_STATUS
    }

    override fun isNoOp(): Boolean = initialBendPoints == finalBendPoints

    /**
     * TODO: Staging Content relevant?
     */
    override fun isContentRelevant(): Boolean = false
}
