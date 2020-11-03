package de.uulm.se.couchedit.client.controller.canvas.gef.operation

import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.gef.mvc.fx.operations.AddContentChildOperation

/**
 * Workaround to disable the index-enforcement of [org.eclipse.gef.mvc.fx.parts.AbstractContentPart.addContentChild]
 * for [ChildrenSupportingPart]s which can insert a content child and determine the index automatically.
 */
class ChildrenSupportingAddContentChildOperation(
        private val parent: ChildrenSupportingPart<*>,
        private val contentChild: Any?
) : AddContentChildOperation(parent, contentChild, -42) {
    override fun execute(monitor: IProgressMonitor?, info: IAdaptable?): IStatus {
        if (parent.content != null && !parent.contentChildrenUnmodifiable.contains(contentChild)) {
            parent.addContentChild(contentChild)
        }

        return Status.OK_STATUS
    }
}
