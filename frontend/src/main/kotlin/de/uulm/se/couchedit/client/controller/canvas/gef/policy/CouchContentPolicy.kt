package de.uulm.se.couchedit.client.controller.canvas.gef.policy

import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import de.uulm.se.couchedit.client.controller.canvas.gef.operation.ChildrenSupportingAddContentChildOperation
import de.uulm.se.couchedit.client.controller.canvas.gef.part.system.RootDrawingPart
import org.eclipse.gef.mvc.fx.operations.AttachToContentAnchorageOperation
import org.eclipse.gef.mvc.fx.operations.DetachFromContentAnchorageOperation
import org.eclipse.gef.mvc.fx.operations.RemoveContentChildOperation
import org.eclipse.gef.mvc.fx.parts.IContentPart
import org.eclipse.gef.mvc.fx.policies.ContentPolicy

/**
 * Custom [ContentPolicy] implementation which executes index-less adding for [ChildrenSupportingPart]s.
 */
class CouchContentPolicy : ContentPolicy() {
    private val rootDrawingPart by lazy {
        host.root.childrenUnmodifiable[0] as RootDrawingPart
    }

    override fun addContentChild(contentChild: Any?, index: Int) {
        val host = this.host

        if (host is ChildrenSupportingPart<*>) {
            checkInitialized()
            compositeOperation.add(ChildrenSupportingAddContentChildOperation(host, contentChild))
            locallyExecuteOperation()
            return
        }
        super.addContentChild(contentChild, index)
    }

    fun changeParent(newParent: ChildrenSupportingPart<*>?) {
        val actualNewParent = newParent ?: rootDrawingPart

        val includedPart = host

        val content = includedPart.content

        // get items currently anchored to the part to be moved
        val anchoreds = includedPart.anchoredsUnmodifiable
        val oldParent = includedPart.parent as? IContentPart<*>

        val contentAnchoreds = mutableListOf<Pair<IContentPart<*>, String>>()

        if (oldParent !== null) {
            for (anchored in anchoreds) {
                val roles = anchored.anchoragesUnmodifiable.get(includedPart)

                if (anchored is IContentPart<*>) {
                    for (role in roles) {
                        compositeOperation.add(DetachFromContentAnchorageOperation(
                                anchored,
                                includedPart,
                                role
                        ))
                        contentAnchoreds.add(Pair(anchored, role))
                    }
                }
            }

            // Now remove from old parent
            compositeOperation.add(RemoveContentChildOperation(oldParent, content))
        }

        // And add it to new one
        compositeOperation.add(ChildrenSupportingAddContentChildOperation(actualNewParent, content))

        // then re-attach anchorages.
        for ((anchored, role) in contentAnchoreds) {
            compositeOperation.add(AttachToContentAnchorageOperation(anchored, content, role))
        }

        locallyExecuteOperation()
    }
}
