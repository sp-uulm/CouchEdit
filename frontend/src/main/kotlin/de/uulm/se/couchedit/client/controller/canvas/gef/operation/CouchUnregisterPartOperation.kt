package de.uulm.se.couchedit.client.controller.canvas.gef.operation;

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

class CouchUnregisterPartOperation(val part: BasePart<*, *>, val viewer: IViewer) : AbstractOperation("CouchEdit: Unregister content part"), ITransactionalOperation {
    private val operationHandler by lazy {
        viewer.getAdapter(CanvasOperationHandler::class.java)
    }

    private val canvasCoordinator by lazy {
        viewer.getAdapter(CanvasCoordinator::class.java)
    }

    private val registeredPart: BasePart<*, *>?

    private var wasActive = false

    init {
        registeredPart = part.getContent()?.id?.let(canvasCoordinator::getPartById)
    }

    override fun execute(monitor: IProgressMonitor?, adaptable: IAdaptable?): IStatus {
        wasActive = operationHandler.removePart(part)

        return Status.OK_STATUS
    }

    override fun redo(monitor: IProgressMonitor?, adaptable: IAdaptable?) = execute(monitor, adaptable)

    override fun undo(monitor: IProgressMonitor?, adaptable: IAdaptable?): IStatus {
        operationHandler.registerPart(part, wasActive)

        return Status.OK_STATUS
    }

    override fun isNoOp() = registeredPart !== part

    override fun isContentRelevant() = true
}
