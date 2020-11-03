package de.uulm.se.couchedit.client.controller.canvas.gef.policy

import com.google.common.collect.SetMultimap
import de.uulm.se.couchedit.client.controller.canvas.gef.operation.CouchRegisterPartOperation
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.IContentPart
import org.eclipse.gef.mvc.fx.parts.IVisualPart
import org.eclipse.gef.mvc.fx.policies.CreationPolicy

class CreationAndRegistrationPolicy : CreationPolicy() {
    override fun create(
            content: Any?,
            parent: IVisualPart<out Node>?,
            index: Int,
            anchoreds: SetMultimap<IContentPart<out Node>, String>?,
            doFocus: Boolean,
            doSelect: Boolean
    ): IContentPart<out Node> {
        return create(content, parent, index, anchoreds, doFocus, doSelect, true)
    }

    /**
     * Works like the regular [CreationAndRegistrationPolicy.create] function. However, in contrast to that function,
     * if the [doActivate] flag is not set, the new part will not be activated, instead the calling procedure will have
     * to take on this task itself.
     *
     * @see create
     */
    fun create(
            content: Any?,
            parent: IVisualPart<out Node>?,
            index: Int,
            anchoreds: SetMultimap<IContentPart<out Node>, String>?,
            doFocus: Boolean,
            doSelect: Boolean,
            doActivate: Boolean
    ): IContentPart<out Node> {
        val part = super.create(content, parent, index, anchoreds, doFocus, doSelect)

        (part as? BasePart<*, *>)?.let {
            this.compositeOperation.add(CouchRegisterPartOperation(it, host.root.viewer, doActivate))
        }

        return part
    }
}
