package de.uulm.se.couchedit.client.controller.canvas.gef

import de.uulm.se.couchedit.client.util.collections.GraphicObjectZOrderSetImpl
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import javafx.collections.ObservableList
import javafx.scene.Node
import org.eclipse.gef.mvc.fx.parts.AbstractContentPart
import java.lang.reflect.Field

abstract class ChildrenSupportingPart<V : Node> : AbstractContentPart<V>() {
    private val contentChildren = GraphicObjectZOrderSetImpl()

    private var superContentChildrenInternal: ObservableList<Any> = hackGetParentContentChildrenList()

    /**
     * HACK: GEF has a check in place whether the insertion order of child elements is correct.
     * We don't need that as the
     *
     * @see AbstractContentPart.addContentChild
     * @todo Find a proper solution for this.
     */
    private fun hackGetParentContentChildrenList(): ObservableList<Any> {
        /* yes, I know. As long as the GEF implementation does not change it is fine */
        @Suppress("UNCHECKED_CAST")
        return superContentChildrenInternalField.get(this) as ObservableList<Any>
    }

    /**
     * Adds a content child without checking the insertion order.
     */
    fun addContentChild(contentChild: Any?) {
        synchronized(this) {
            doAddContentChild(contentChild)

            val contentChildren = this.doGetContentChildren()

            // There seems to be a bug in Java-Kotlin interop here with ambiguous parameters vararg vs. Collection :(
            superContentChildrenInternal.setAll(*contentChildren.toTypedArray())
        }
    }

    /**
     * Adds the given [GraphicObject] [contentChild] to this part. The [index] is ignored as the elements are sorted by
     * their [GraphicObject.z] property.
     */
    override fun doAddContentChild(contentChild: Any?, index: Int) {
        this.doAddContentChild(contentChild)
    }

    fun doAddContentChild(contentChild: Any?) {
        (contentChild as? GraphicObject<*>)?.let {
            this.contentChildren.add(it)
        } ?: throw IllegalArgumentException("Content child $contentChild is not a GraphicObject")
    }

    override fun doRemoveContentChild(contentChild: Any?) {
        this.contentChildren.remove(contentChild)
    }

    override fun doGetContentChildren(): List<GraphicObject<*>> {
        return this.contentChildren.toList()
    }

    override fun refreshContentChildren() {
        this.contentChildren.refreshOrder()
        super.refreshContentChildren()
    }

    fun getElementBehind(element: GraphicObject<*>): GraphicObject<*>? = this.contentChildren.getElementBehind(element)

    fun getElementInFrontOf(element: GraphicObject<*>): GraphicObject<*>? = this.contentChildren.getElementInFrontOf(element)

    fun getForemostElement(): GraphicObject<*> = this.contentChildren.getForemostElement()

    fun getBackmostElement(): GraphicObject<*> = this.contentChildren.getBackmostElement()

    companion object {
        val superContentChildrenInternalField: Field by lazy {
            val clazz = AbstractContentPart::class.java

            val field = try {
                clazz.getDeclaredField("contentChildren")
            } catch (e: NoSuchFieldException) {
                throw IllegalAccessException(
                        "Cannot retrieve contentChildren list from AbstractContentPart." +
                                "GEF Implementation may have changed!"
                )
            }

            field.isAccessible = true

            return@lazy field
        }
    }
}
