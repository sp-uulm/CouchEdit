package de.uulm.se.couchedit.client.controller.canvas.gef.operation

import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import org.eclipse.core.commands.operations.AbstractOperation
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation

class ChangeZOperation(private val part: BasePart<*, *>) : AbstractOperation("Change Z-Order"), ITransactionalOperation {
    val initialZ = part.getContent()?.z?.toList() ?: listOf<Int>()

    var finalZ = initialZ

    override fun execute(monitor: IProgressMonitor?, info: IAdaptable?): IStatus {
        this.setZOfPart(finalZ)

        return Status.OK_STATUS
    }

    override fun undo(monitor: IProgressMonitor?, info: IAdaptable?): IStatus {
        this.setZOfPart(initialZ)

        return Status.OK_STATUS
    }

    override fun redo(monitor: IProgressMonitor?, info: IAdaptable?): IStatus = this.execute(monitor, info)

    override fun isNoOp(): Boolean = finalZ == initialZ

    override fun isContentRelevant(): Boolean = true

    private fun setZOfPart(newZ: List<Int>) {
        part.getContent()?.apply { z = newZ.toMutableList() }
        part.stagingContent?.apply { z = newZ.toMutableList() }

        (part.getParent() as? ChildrenSupportingPart)?.refreshContentChildren()
        part.publishContent(false)
    }
}
