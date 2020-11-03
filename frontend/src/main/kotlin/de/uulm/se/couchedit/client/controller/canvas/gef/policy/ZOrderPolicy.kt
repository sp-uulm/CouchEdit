package de.uulm.se.couchedit.client.controller.canvas.gef.policy

import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import de.uulm.se.couchedit.client.controller.canvas.gef.operation.ChangeZOperation
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import org.eclipse.gef.mvc.fx.operations.AbstractCompositeOperation
import org.eclipse.gef.mvc.fx.operations.ITransactionalOperation
import org.eclipse.gef.mvc.fx.operations.ReverseUndoCompositeOperation
import org.eclipse.gef.mvc.fx.policies.AbstractPolicy

class ZOrderPolicy : AbstractPolicy() {
    fun toForeground() {
        val op = this.createToForegroundOperation()

        if (op != null) {
            getCompositeOperation().add(op)
        }
    }

    fun toBackground() {
        val op = this.createToBackgroundOperation()

        if (op != null) {
            getCompositeOperation().add(op)
        }
    }

    fun oneStepToFront() {
        val op = this.createOneStepToFrontOperation()

        if (op != null) {
            getCompositeOperation().add(op)
        }
    }

    fun oneStepToBack() {
        val op = this.createOneStepToBackOperation()

        if (op != null) {
            getCompositeOperation().add(op)
        }
    }

    override fun createOperation(): ITransactionalOperation = ReverseUndoCompositeOperation("Change ZOrder")

    private fun createToForegroundOperation(): ChangeZOperation? {
        val part = this.getHostAsBasePart() ?: return null
        (part.getParent() as? ChildrenSupportingPart)?.let { parent ->
            val foremost = parent.getForemostElement()

            val newZ = listOf((foremost.z.firstOrNull() ?: 0) + 1)

            val ret = ChangeZOperation(part)
            ret.finalZ = newZ

            return ret
        } ?: return null
    }

    private fun createToBackgroundOperation(): ChangeZOperation? {
        val part = this.getHostAsBasePart() ?: return null
        (part.getParent() as? ChildrenSupportingPart)?.let { parent ->
            val backmost = parent.getBackmostElement()

            val newZ = listOf((backmost.z.firstOrNull() ?: 0) - 1)

            val ret = ChangeZOperation(part)
            ret.finalZ = newZ

            return ret
        } ?: return null
    }

    private fun createOneStepToBackOperation(): ChangeZOperation? {
        val part = this.getHostAsBasePart() ?: return null
        (part.getParent() as? ChildrenSupportingPart<*>)?.let { parent ->
            // the element which the given part should be placed in front of
            val oneBehind = part.getContent()?.let(parent::getElementBehind)

            // the element which the given part should be placed behind
            val twoBehind = oneBehind?.let(parent::getElementBehind)

            if (oneBehind == null || twoBehind == null) {
                return createToBackgroundOperation()
            }

            val commonPrefix = this.getCommonPrefix(oneBehind.z, twoBehind.z).toMutableList()

            val decidingValueOne = oneBehind.z.getOrNull(commonPrefix.size) ?: 0
            val decidingValueTwo = twoBehind.z.getOrNull(commonPrefix.size) ?: 0

            val finalZ = if (decidingValueOne - decidingValueTwo == 1) {
                // difference between the two elements is 1. That means there is no "space" to put the Z coordinate
                // between the two => Have to create a new level

                oneBehind.z.toMutableList().apply {
                    add(-1)
                }
            } else {
                commonPrefix.apply { add(decidingValueOne - 1) }
            }

            val ret = ChangeZOperation(part)
            ret.finalZ = finalZ

            return ret
        }

        return null
    }

    private fun createOneStepToFrontOperation(): ChangeZOperation? {
        val part = this.getHostAsBasePart() ?: return null
        (part.getParent() as? ChildrenSupportingPart<*>)?.let { parent ->
            // the element which the given part should be placed in front of
            val oneInFrontOf = part.getContent()?.let(parent::getElementInFrontOf)

            // the element which the given part should be placed behind
            val twoInFrontOf = oneInFrontOf?.let(parent::getElementInFrontOf)

            if (oneInFrontOf == null || twoInFrontOf == null) {
                return createToForegroundOperation()
            }

            val commonPrefix = this.getCommonPrefix(oneInFrontOf.z, twoInFrontOf.z).toMutableList()

            val decidingValueOne = oneInFrontOf.z.getOrNull(commonPrefix.size) ?: 0
            val decidingValueTwo = twoInFrontOf.z.getOrNull(commonPrefix.size) ?: 0

            val finalZ = if (decidingValueTwo - decidingValueOne == 1) {
                // difference between the two elements is 1. That means there is no "space" to put the Z coordinate
                // between the two => Have to create a new level

                oneInFrontOf.z.toMutableList().apply {
                    add(1)
                }
            } else {
                commonPrefix.apply { add(decidingValueOne + 1) }
            }

            val ret = ChangeZOperation(part)
            ret.finalZ = finalZ

            return ret
        }

        return null
    }

    fun getCommonPrefix(z1: List<Int>, z2: List<Int>): List<Int> {
        val iterator = z2.iterator()

        return z1.takeWhile {
            val otherValue = if (!iterator.hasNext()) 0 else iterator.next()
            return@takeWhile it == otherValue
        }
    }

    protected fun getCompositeOperation(): AbstractCompositeOperation {
        return operation as AbstractCompositeOperation
    }

    protected fun getHostAsBasePart(): BasePart<*, *>? {
        return this.host as? BasePart<*, *>
    }
}
