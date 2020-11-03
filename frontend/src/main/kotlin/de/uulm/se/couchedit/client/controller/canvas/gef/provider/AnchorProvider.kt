package de.uulm.se.couchedit.client.controller.canvas.gef.provider

import com.google.common.reflect.TypeToken
import com.google.inject.Provider
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.scene.Node
import org.eclipse.gef.common.adapt.IAdaptable
import org.eclipse.gef.fx.anchors.DynamicAnchor
import org.eclipse.gef.fx.anchors.IAnchor
import org.eclipse.gef.geometry.planar.IGeometry
import org.eclipse.gef.mvc.fx.parts.IVisualPart

/**
 * Provider / factory for anchors attaching connections to their elements
 */
class AnchorProvider : IAdaptable.Bound.Impl<IVisualPart<out Node>>(), Provider<IAnchor> {
    private val anchor: IAnchor by lazy {
        // element the anchor will be attached to
        val anchorage = adaptable.visual

        val ret = DynamicAnchor(anchorage)

        ret.getComputationParameter(DynamicAnchor.AnchorageReferenceGeometry::class.java).bind(object : ObjectBinding<IGeometry>() {
            init {
                bind(anchorage.layoutBoundsProperty())
            }

            override fun computeValue(): IGeometry {
                return adaptable.getAdapter(object : TypeToken<Provider<IGeometry>>() {}).get()
            }
        })

        return@lazy ret
    }

    override fun adaptableProperty(): ReadOnlyObjectProperty<IVisualPart<out Node>>? = null

    override fun get() = this.anchor
}
