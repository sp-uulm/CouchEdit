package de.uulm.se.couchedit.client.controller.canvas.gef.policy

import de.uulm.se.couchedit.client.controller.canvas.gef.operation.CouchUnregisterPartOperation
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.IContentPart
import org.eclipse.gef.mvc.fx.policies.DeletionPolicy

class DeletionAndDeregistrationPolicy : DeletionPolicy() {
    override fun delete(contentPartToDelete: IContentPart<out Node>?) {
        super.delete(contentPartToDelete)

        (contentPartToDelete as? BasePart<*, *>)?.let {
            compositeOperation.add(CouchUnregisterPartOperation(contentPartToDelete, host.root.viewer))
        }
    }

}
