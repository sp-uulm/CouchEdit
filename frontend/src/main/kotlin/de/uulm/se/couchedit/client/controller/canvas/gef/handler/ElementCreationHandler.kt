package de.uulm.se.couchedit.client.controller.canvas.gef.handler

import com.google.common.collect.HashMultimap
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.CreationAndRegistrationPolicy
import de.uulm.se.couchedit.client.util.gef.rootDrawingPart
import de.uulm.se.couchedit.model.base.Element
import org.eclipse.gef.mvc.fx.handlers.AbstractHandler
import org.eclipse.gef.mvc.fx.policies.CreationPolicy

/**
 * [AbstractHandler] that is in charge of creating Elements on the canvas. This is usually executed from Tools via the
 * ToolManagingBehavior.
 */
class ElementCreationHandler : AbstractHandler() {
    private val rootDrawingPart by lazy { host.root.viewer.rootDrawingPart }

    private val creationPolicy by lazy { host.root.getAdapter(CreationAndRegistrationPolicy::class.java) }

    /**
     * Uses the [CreationPolicy] bound to the RootDrawingPart to insert a Set of [Element]s to the application data model.
     */
    fun createElements(elements: Set<Element>) {
        // initialize the policy
        init(creationPolicy)
        // create a IContentPart for our new model. We don't use the
        // returned content-part
        for (element in elements) {
            creationPolicy.create(
                    element,
                    rootDrawingPart,
                    rootDrawingPart.childrenUnmodifiable.size,
                    HashMultimap.create(),
                    doFocus = false,
                    doSelect = false,
                    doActivate = true
            )
        }
        // execute the creation
        commit(creationPolicy)
    }
}
