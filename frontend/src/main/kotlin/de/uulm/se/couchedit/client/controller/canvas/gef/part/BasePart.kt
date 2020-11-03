package de.uulm.se.couchedit.client.controller.canvas.gef.part

import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import com.google.inject.Inject
import de.uulm.se.couchedit.client.controller.canvas.gef.ChildrenSupportingPart
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasOperationHandler
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.BaseVisual
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.visual.RootVisual
import de.uulm.se.couchedit.model.attribute.Attribute
import de.uulm.se.couchedit.model.attribute.AttributeReference
import de.uulm.se.couchedit.model.attribute.elements.AttributeBag
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import javafx.scene.Node
import javafx.scene.transform.Affine
import org.eclipse.gef.mvc.fx.parts.IVisualPart

/**
 * ContentParts are the "Controllers" of GEF.
 * They are registered with the main [org.eclipse.gef.mvc.fx.viewer.IViewer] component and connect the viewer actions
 * on visuals (= JavaFX components displayed on screen) to the application model and vice versa.
 *
 * They are created on demand by the [de.uulm.se.couchedit.client.controller.canvas.gef.ContentPartFactory], which makes
 * use of the Guice Injector so Parts can take advantage of all injectable services.
 *
 * The BasePart class includes facilities to take care of communicating changes to the rest of the CouchEdit
 * application and converting the local coordinates of the element into global coordinates.
 *
 * The parameter [V] specifies the type of Visual that is managed by this Part, while [C] describes the type of content
 * (model) that is represented.
 */
abstract class BasePart<V, C> : ChildrenSupportingPart<V>() where V : Node, V : BaseVisual, C : Shape {
    internal lateinit var operationHandler: CanvasOperationHandler
        private set

    val attributes = HashMultimap.create<Class<out AttributeBag>, AttributeBag>()!!

    var contentId: String? = null
        private set

    /**
     * This is the state of the content in live form as it happens on the canvas.
     */
    var stagingContent: GraphicObject<C>? = null

    @Inject
    internal fun setOperationHandler(operationHandler: CanvasOperationHandler) {
        this.operationHandler = operationHandler
    }

    @Suppress("UNCHECKED_CAST")
    override fun setContent(content: Any?) {
        if (content == null) {
            super.setContent(content)

            return
        }

        (content as? GraphicObject<C>)?.let {
            this.contentId = content.id

            super.setContent(content)

            this.stagingContent = (content.copy() as GraphicObject<C>)
        } ?: run {
            val invalidClass = content.javaClass.name

            throw IllegalArgumentException("Invalid content type: $invalidClass")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContent() = super.getContent() as GraphicObject<C>?

    override fun doGetContentAnchorages(): SetMultimap<out Any, String> = HashMultimap.create()

    /**
     * Adds the given [child] visual (JavaFX) node to the children of this part's visual.
     */
    override fun doAddChildVisual(child: IVisualPart<out Node>?, index: Int) {
        child?.visual?.let { this.visual.addChild(it, index) }
    }

    /**
     * Removes the given [child] visual (JavaFX) node from the children of this part.
     */
    override fun doRemoveChildVisual(child: IVisualPart<out Node>?, index: Int) {
        child?.visual?.let { this.visual.removeChild(it) }
    }

    /**
     * Publishes the current state of the content (and all dependant parts' content) into the rest of the system.
     */
    fun publishContent(staging: Boolean) {
        this.operationHandler.triggerContentUpdate(this, staging)
    }

    fun getAllContentsInTree(staging: Boolean): Set<Pair<BasePart<*, *>, GraphicObject<*>>> {
        val toPublish = if (staging) this.stagingContent else this.content

        val elements = this.getDependantPartsWithContent(staging).toMutableSet()

        // ensure that the contents correspond to their visual
        elements.forEach { (part, _) -> part.updateContentFromVisual(true) }

        toPublish?.let { elements.add(Pair(this, it)) }

        return elements
    }

    /**
     * Gets all parts that depend on this part with their associated contents through all levels.
     *
     * Currently this gets the children and anchored parts.
     */
    protected fun getDependantPartsWithContent(staging: Boolean): Set<Pair<BasePart<*, *>, GraphicObject<*>>> {
        val partToPair = { childPart: IVisualPart<*> ->
            (childPart as? BasePart<*, *>)?.let {
                Pair(it, (if (staging) it.stagingContent else it.getContent()) ?: return@let null)
            }
        }

        val foundParts = mutableSetOf<BasePart<*, *>>()

        var nextParts = mutableListOf<BasePart<*, *>>()

        do {
            val currentParts = if (nextParts.isEmpty()) listOf(this) else nextParts

            nextParts = mutableListOf()

            for (part in currentParts) {
                if (part in foundParts) {
                    continue
                }

                nextParts.addAll(part.getChildrenUnmodifiable().filterIsInstance(BasePart::class.java))
                nextParts.addAll(part.getAnchoredsUnmodifiable().mapNotNull {
                    if (it == this) {
                        return@mapNotNull null
                    }

                    return@mapNotNull it as? BasePart<*, *>
                })

                if (part != this) {
                    foundParts.add(part)
                }
            }
        } while (nextParts.isNotEmpty())

        return foundParts.mapNotNull(partToPair).toSet()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (this.content == null) {
            return false
        }

        return this.content == (other as? BasePart<*, *>)?.getContent()
    }

    override fun hashCode(): Int {
        return this.javaClass.hashCode() + 31 * this.content.hashCode()
    }

    /**
     * Sets the state of the content from the state of the [visual] on the canvas, e.g. when parent visuals change.
     *
     * If [staging] is set, only the [staging] content instance will be updated.
     */
    fun updateContentFromVisual(staging: Boolean) {
        this.stagingContent?.shape?.let(this::doUpdateContentInstance)

        if (!staging) {
            this.content?.shape?.let(this::doUpdateContentInstance)
        }
    }

    /**
     * Callback after the [attributes] have been changed - should apply the changes to the element's visuals.
     */
    open fun doRefreshAttributes() {}

    /**
     * Hook method for when a [content] instance needs updating. Subclasses should update [content] based on the
     * information from their [visual] when this is called
     */
    protected open fun doUpdateContentInstance(content: C) {}

    /* ------ coordinate helpers ----
     * as CouchEdit uses the "global" canvas coordinate system for all Elements, whether they are included in another
     * element or not (which is just another relation that can change at any moment) we don't want the local / parent
     * dependant coordinate system that GEF / JavaFX use.
     * Coordinates are to be always read from the (0,0) spot of the main canvas. Because of that for correct rendering,
     * we need to convert them. Those utility functions achieve just that.
     */


    /**
     * Calculates the coordinates of the content in the global canvas based on the given local [visualTransform].
     */
    protected fun calculateCanvasContentCoordinates(visualTransform: Affine): Affine {
        if (visual.parent == null) {
            return visualTransform
        }

        val parentTransform = getRootDrawingTransform(visual.parent ?: return visualTransform)

        parentTransform.append(visualTransform)

        return parentTransform
    }

    /**
     * Calculates the coordinates of the content in the local parent element based on the given global canvas
     * [contentTransform].
     */
    protected fun calculateParentVisualCoordinates(contentTransform: Affine): Affine {
        if (visual.parent == null) {
            return contentTransform
        }

        val parentCoordinateSystemTransform = getRootDrawingTransform(visual.parent ?: return contentTransform)

        parentCoordinateSystemTransform.invert()

        parentCoordinateSystemTransform.append(contentTransform)

        return parentCoordinateSystemTransform
    }

    /**
     * Gets the base transform to convert the coordinate system of the elements inside the given [visual] to the global
     * canvas coordinate system.
     */
    fun getRootDrawingTransform(visual: Node): Affine {
        var current = visual

        val currentTransform = Affine()

        while (current !is RootVisual) {
            currentTransform.append(current.localToParentTransform)

            current = current.parent ?: throw IllegalStateException("$visual was not in the tree of an RootDrawing")
        }

        return currentTransform
    }

    /*
     * ------ attribute helpers ----
     */

    /**
     * Gets the value of the attribute specified by [ref] in the first bag of type [bagType] (or one of its subtypes)
     * that contains such an attribute.
     *
     * If no matching attribute is found in any Bag contained in [attributes], then <code>null</code> is returned.
     */
    fun <T : Attribute<*>> getAttributeValueFromBags(bagType: Class<out AttributeBag>, ref: AttributeReference<T>): T? {
        for ((availableBagType, bagSet) in this.attributes.asMap()) {
            if (!bagType.isAssignableFrom(availableBagType)) {
                continue
            }

            for (bag in bagSet) {
                return bag[ref] ?: continue
            }
        }

        return null
    }
}
