package de.uulm.se.couchedit.client.controller.canvas.gef.policy.text

import de.uulm.se.couchedit.client.controller.canvas.gef.operation.CommitTextOperation
import de.uulm.se.couchedit.client.controller.canvas.gef.part.TextEditModePart
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation
import org.eclipse.gef.mvc.fx.policies.AbstractPolicy

/**
 * Policy to manage editing of text in [TextEditModePart]s.
 *
 * The Operation used is a [CommitTextOperation].
 */
class TextEditPolicy : AbstractPolicy() {
    override fun createOperation(): ITransactionalOperation {
        return (this.host as? TextEditModePart)?.let {
            return CommitTextOperation(it)
        } ?: throw IllegalArgumentException("TextEditPolicy only works on TextEditModeParts")
    }

    override fun init() {
        super.init()

        this.getCommitTextOperation()?.part?.startEditing()
    }

    private fun getCommitTextOperation() = this.operation as? CommitTextOperation
}
