package de.uulm.se.couchedit.client.controller.canvas.gef.operation

import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasCoordinator
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasOperationHandler
import org.eclipse.core.commands.operations.AbstractOperation
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation
import org.eclipse.gef.mvc.fx.viewer.IViewer

class CouchRegisterPartOperation(
        val part: BasePart<*, *>,
        val viewer: IViewer,
        val activate: Boolean
) : AbstractOperation("CouchEdit: Register Content Part"), ITransactionalOperation {
    private val operationHandler by lazy {
        viewer.getAdapter(CanvasOperationHandler::class.java)
    }

    private val canvasCoordinator by lazy {
        viewer.getAdapter(CanvasCoordinator::class.java)
    }

    /**
     * ContentPart already registered in the CanvasCoordinator for the ID of [part]'s content
     */
    private val registeredPart: BasePart<*, *>?

    init {
        registeredPart = part.getContent()?.id?.let(canvasCoordinator::getPartById)
    }

    override fun execute(monitor: IProgressMonitor?, adaptable: IAdaptable?): IStatus {
        operationHandler.registerPart(part, activate)

        return Status.OK_STATUS
    }

    override fun redo(monitor: IProgressMonitor?, adaptable: IAdaptable?) = execute(monitor, adaptable)

    override fun undo(monitor: IProgressMonitor?, adaptable: IAdaptable?): IStatus {
        operationHandler.removePart(part)

        return Status.OK_STATUS
    }

    override fun isNoOp() = registeredPart === part

    override fun isContentRelevant() = true
}
