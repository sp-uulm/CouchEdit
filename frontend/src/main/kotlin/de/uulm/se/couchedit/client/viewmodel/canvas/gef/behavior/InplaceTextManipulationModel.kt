package de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior

import de.uulm.se.couchedit.client.controller.canvas.gef.part.TextEditModePart
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.text.TextEditPolicy
import org.eclipse.gef.mvc.fx.domain.IDomain

/**
 * Model storing the current state of text editing of the front end component.
 */
internal class InplaceTextManipulationModel {
    /**
     * The domain in which the [currentPolicy] should be committed on execution.
     *
     * We need to keep reference on this Policy as the operation may be finished by a handler bound to another Part
     * than it was started.
     */
    private var domain: IDomain? = null

    /**
     * The (uncommitted) operation currently underway
     */
    private var currentPolicy: TextEditPolicy? = null

    /**
     * The part for which the edit mode is currently active.
     */
    val part: TextEditModePart<*>?
        get() = currentPolicy?.host as? TextEditModePart

    /**
     * Checks whether there is currently a [TextEditModePart] being edited, or if the [part] parameter is given checks
     * whether this specific part is the one currently being edited.
     */
    fun isEditing(part: TextEditModePart<*>? = null): Boolean {
        if (part == null) {
            return this.currentPolicy != null
        }

        return this.part == part
    }

    /**
     * Starts editing the given [part].
     */
    fun startEditing(part: TextEditModePart<*>, domain: IDomain) {
        if (this.isEditing()) {
            this.abortEditing()
        }

        val policy = part.getAdapter(TextEditPolicy::class.java)

        this.domain = domain
        this.currentPolicy = policy

        policy.init()
    }

    fun abortEditing() {
        this.part?.stopEditing()

        this.currentPolicy?.rollback()
        this.currentPolicy = null
    }

    fun commitEditing() {
        val operation = currentPolicy?.commit()
        this.domain?.apply {
            if (operation?.isNoOp == false) {
                execute(operation, null)
            }
        }

        this.part?.stopEditing()
        currentPolicy = null
    }
}

