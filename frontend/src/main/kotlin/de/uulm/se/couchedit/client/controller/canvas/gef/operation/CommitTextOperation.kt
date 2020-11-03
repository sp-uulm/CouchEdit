package de.uulm.se.couchedit.client.controller.canvas.gef.operation

import de.uulm.se.couchedit.client.controller.canvas.gef.part.TextEditModePart
import org.eclipse.core.commands.operations.AbstractOperation
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation

/**
 * Operation that is executed whenever the text of a [Label] changes.
 */
internal class CommitTextOperation(internal val part: TextEditModePart<*>) : AbstractOperation("Change text of \"${part.text}\""), ITransactionalOperation {
    val initialText = part.text

    /**
     * Tracks the currently edited text while the edit operation is still in progress
     */
    var stagingText = part.stagingTextProperty

    var finalText = part.text

    override fun execute(p0: IProgressMonitor?, p1: IAdaptable?): IStatus {
        this.finalText = part.stopEditing()

        part.text = this.finalText

        return Status.OK_STATUS
    }

    override fun redo(p0: IProgressMonitor?, p1: IAdaptable?): IStatus {
        part.text = this.finalText

        return Status.OK_STATUS
    }

    override fun undo(p0: IProgressMonitor?, p1: IAdaptable?): IStatus {
        part.text = this.initialText

        return Status.OK_STATUS
    }

    override fun isNoOp(): Boolean = this.stagingText.value == initialText

    override fun isContentRelevant(): Boolean = true
}
