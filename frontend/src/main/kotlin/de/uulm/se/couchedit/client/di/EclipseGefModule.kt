package de.uulm.se.couchedit.client.di

import com.google.inject.Singleton
import com.google.inject.multibindings.MapBinder
import de.uulm.se.couchedit.client.controller.canvas.gef.ContentPartFactory
import de.uulm.se.couchedit.client.controller.canvas.gef.behavior.ToolFeedbackBehavior
import de.uulm.se.couchedit.client.controller.canvas.gef.behavior.ToolManagingBehavior
import de.uulm.se.couchedit.client.controller.canvas.gef.handler.*
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BoundHandleResizePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.TextEditModePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.feedback.FeedbackPartFactory
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.*
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.text.TextEditPolicy
import de.uulm.se.couchedit.client.controller.canvas.gef.provider.AnchorProvider
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasCoordinator
import de.uulm.se.couchedit.client.controller.canvas.processing.CanvasOperationHandler
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.InplaceTextManipulationModel
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.behavior.ToolModel
import org.eclipse.gef.common.adapt.AdapterKey
import org.eclipse.gef.common.adapt.inject.AdapterMaps
import org.eclipse.gef.mvc.fx.MvcFxModule
import org.eclipse.gef.mvc.fx.domain.HistoricizingDomain
import org.eclipse.gef.mvc.fx.domain.IDomain
import org.eclipse.gef.mvc.fx.handlers.BendFirstAnchorageOnSegmentHandleDragHandler
import org.eclipse.gef.mvc.fx.handlers.HoverOnHoverHandler
import org.eclipse.gef.mvc.fx.handlers.ResizeTranslateFirstAnchorageOnHandleDragHandler
import org.eclipse.gef.mvc.fx.handlers.TranslateSelectedOnDragHandler
import org.eclipse.gef.mvc.fx.parts.*
import org.eclipse.gef.mvc.fx.policies.ResizePolicy
import org.eclipse.gef.mvc.fx.policies.TransformPolicy
import org.eclipse.gef.mvc.fx.providers.DefaultAnchorProvider
import org.eclipse.gef.mvc.fx.providers.ShapeBoundsProvider
import org.eclipse.gef.mvc.fx.providers.ShapeOutlineProvider

/**
 * The Guice Module to configure the dependency tree of Eclipse GEF5.
 *
 * GEF makes extensive use of the [MapBinder], which is used to collect services assigned to "roles" that can then be
 * injected to the GEF components.
 */
class EclipseGefModule : MvcFxModule() {
    override fun bindIDomain() {
        this.binder().bind(IDomain::class.java).to(HistoricizingDomain::class.java).`in`(Singleton::class.java)
    }

    override fun bindIContentPartFactoryAsContentViewerAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        // bind ContentPartFactory adapter to the content viewer
        // A map binder "collects" bindings, afterwards they are injected as a whole into the target object.
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ContentPartFactory::class.java)
    }

    override fun bindAbstractContentPartAdapters(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        super.bindAbstractContentPartAdapters(adapterMapBinder)

        // binding the HoverOnHoverPolicy to every part
        // if a mouse is moving above a part it is set i the HoverModel
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(HoverOnHoverHandler::class.java)

        // add the focus and select policy to every part, listening to clicks
        // and changing the focus and selection model
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(CouchFocusAndSelectOnClickHandler::class.java)
    }

    override fun bindMarqueeOnDragHandlerAsIRootPartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.role("0")).to(CouchMarqueeOnDragHandler::class.java)
    }

    override fun bindIRootPartAdaptersForContentViewer(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        super.bindIRootPartAdaptersForContentViewer(adapterMapBinder)

        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ToolManagingBehavior::class.java)
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ToolFeedbackBehavior::class.java)
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ElementCreationHandler::class.java)

        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ToolInteractionHandler::class.java)

        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(TextEditModePartActionHandler::class.java)
    }

    override fun bindFocusAndSelectOnClickHandlerAsIRootPartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(CouchFocusAndSelectOnClickHandler::class.java)
    }

    /**
     * Binds all services internally used by GEF to provide interaction behaviors in the editor canvas.
     */
    private fun bindCommonBehaviorAdapters(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        with(adapterMapBinder) {
            // bind the anchor provider to connect a line to another element
            addBinding(AdapterKey.defaultRole()).to(AnchorProvider::class.java)
            addBinding(AdapterKey.defaultRole()).to(DefaultAnchorProvider::class.java)

            adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ToolInteractionHandler::class.java)

            // bind a geometry provider, which is used e.g. by the anchor provider
            addBinding(AdapterKey.defaultRole()).to(ShapeOutlineProvider::class.java)

            // provides a hover feedback to the shape, used by the HoverBehavior
            var role = AdapterKey.role(DefaultHoverFeedbackPartFactory.HOVER_FEEDBACK_GEOMETRY_PROVIDER)
            addBinding(role).to(ShapeOutlineProvider::class.java)

            // provides a selection feedback to the shape
            role = AdapterKey.role(DefaultSelectionFeedbackPartFactory.SELECTION_FEEDBACK_GEOMETRY_PROVIDER)
            addBinding(role).to(ShapeOutlineProvider::class.java)

            // support moving nodes via mouse drag
            // (TODO: Implementation so that all drag moves will be pushed to the model, not only after the user lets go)
            addBinding(AdapterKey.defaultRole()).to(TransformPolicy::class.java)
            addBinding(AdapterKey.defaultRole()).to(TranslateSelectedOnDragHandler::class.java)

            addBinding(AdapterKey.defaultRole()).to(ShowGraphicObjectContextMenuOnClickPolicy::class.java)

            addBinding(AdapterKey.defaultRole()).to(ResizePolicy::class.java)

            adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(ZOrderPolicy::class.java)

            adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(TextEditModePartActionHandler::class.java)
        }
    }

    /**
     * Bind Provider ensuring that [BoundHandleResizePart]s get resize handles at their edges.
     */
    private fun bindBoundHandleResizableShapeBehavior(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        with(adapterMapBinder) {
            // provides drag handles for resizing to the edges of the shape
            val role = AdapterKey.role(DefaultSelectionHandlePartFactory.SELECTION_HANDLES_GEOMETRY_PROVIDER)
            addBinding(role).to(ShapeBoundsProvider::class.java)
        }
    }

    /**
     * Bind Provider ensuring that all [IBendableContentPart]s have bend handles attached to their segments.
     */
    private fun bindLineBehavior(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        with(adapterMapBinder) {
            val role = AdapterKey.role(DefaultSelectionHandlePartFactory.SELECTION_HANDLES_GEOMETRY_PROVIDER)
            addBinding(role).to(ShapeOutlineProvider::class.java)

            addBinding(AdapterKey.defaultRole()).to(NoAutoconnectBendConnectionPolicy::class.java)
        }
    }

    override fun bindCreationPolicyAsIRootPartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(CreationAndRegistrationPolicy::class.java)
    }

    override fun bindDeletionPolicyAsIRootPartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(DeletionAndDeregistrationPolicy::class.java)
    }

    override fun bindIViewerAdaptersForContentViewer(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        super.bindIViewerAdaptersForContentViewer(adapterMapBinder)

        val role = AdapterKey.role(ToolFeedbackBehavior.ROLE_TOOL_FEEDBACK_PART_FACTORY)

        with(adapterMapBinder) {
            addBinding(role).to(FeedbackPartFactory::class.java)

            addBinding(AdapterKey.defaultRole()).to(CanvasCoordinator::class.java)
            addBinding(AdapterKey.defaultRole()).to(CanvasOperationHandler::class.java)

            addBinding(AdapterKey.defaultRole()).to(ToolModel::class.java)
            addBinding(AdapterKey.defaultRole()).to(InplaceTextManipulationModel::class.java)
        }
    }

    override fun bindContentPolicyAsAbstractContentPartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(CouchContentPolicy::class.java)
    }

    private fun bindEditablePartBehavior(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole()).to(TextEditPolicy::class.java)
    }

    /**
     * Binds the parts of the selection handles (the squares in the corner of all [BoundHandleResizePart]s) to the
     * policy that allows them to resize their respective Parts.
     */
    private fun bindResizeHandlePartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole())
                .to(ResizeTranslateFirstAnchorageOnHandleDragHandler::class.java)
    }

    /**
     * Binds the round handles of line segment to the policy that lets the user bind them.
     */
    private fun bindBendHandlePartAdapter(adapterMapBinder: MapBinder<AdapterKey<*>, Any>) {
        adapterMapBinder.addBinding(AdapterKey.defaultRole())
                .to(BendFirstAnchorageOnSegmentHandleDragHandler::class.java)
    }

    override fun configure() {
        // start the default configuration
        super.configure()

        bindCommonBehaviorAdapters(AdapterMaps.getAdapterMapBinder(binder(), BasePart::class.java))
        bindBoundHandleResizableShapeBehavior(AdapterMaps.getAdapterMapBinder(binder(), BoundHandleResizePart::class.java))
        bindLineBehavior(AdapterMaps.getAdapterMapBinder(binder(), IBendableContentPart::class.java))

        bindEditablePartBehavior(AdapterMaps.getAdapterMapBinder(binder(), TextEditModePart::class.java))

        // with this binding we create the handles
        bindResizeHandlePartAdapter(
                AdapterMaps.getAdapterMapBinder(binder(), SquareSegmentHandlePart::class.java)
        )

        bindBendHandlePartAdapter(
                AdapterMaps.getAdapterMapBinder(binder(), CircleSegmentHandlePart::class.java)
        )
    }
}
