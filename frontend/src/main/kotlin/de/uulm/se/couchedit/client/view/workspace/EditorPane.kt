package de.uulm.se.couchedit.client.view.workspace

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.uulm.se.couchedit.client.controller.canvas.gef.policy.ZOrderPolicy
import de.uulm.se.couchedit.client.controller.workspace.EditorPaneController
import de.uulm.se.couchedit.client.util.fx.generateView
import de.uulm.se.couchedit.client.util.gef.contentViewer
import de.uulm.se.couchedit.client.view.attribute.AttributeFragment
import de.uulm.se.couchedit.client.view.canvas.GefCanvasView
import de.uulm.se.couchedit.client.view.palette.PaletteView
import de.uulm.se.couchedit.client.viewmodel.canvas.gef.element.RootDrawing
import de.uulm.se.couchedit.debugui.view.DebugView
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.stage.FileChooser
import javafx.stage.StageStyle
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.gef.mvc.fx.domain.IDomain
import org.eclipse.gef.mvc.fx.models.SelectionModel
import org.eclipse.gef.mvc.fx.parts.IContentPart
import org.eclipse.gef.mvc.fx.policies.IPolicy
import tornadofx.*

/**
 * Main editor pane, currently containing:
 * * A [PaletteView] to the left
 * * In the center area, a [splitpane] containing:
 *      * The main Canvas of the [GefCanvasView]
 *      * The [AttributeFragment] allowing change of the Attributes in the GraphicObject currently selected in the
 *        [GefCanvasView]
 */
class EditorPane : View("CouchEdit") {
    val domain: IDomain by di()

    val controller: EditorPaneController by inject()

    override val refreshable: BooleanExpression
        get() = BooleanExpression.booleanExpression(SimpleBooleanProperty(true))

    private val selectionModel = domain.contentViewer.getAdapter(SelectionModel::class.java)

    private var currentZOrderPolicy: ObjectProperty<ZOrderPolicy?> = SimpleObjectProperty(null)

    private val errorHandler: DefaultErrorHandler by di()

    private val attributeFragment = find<AttributeFragment>()

    override val root = borderpane {
        left<PaletteView>()

        center = splitpane(Orientation.HORIZONTAL) {
            add<GefCanvasView>()
            add(attributeFragment)
        }

    }

    override fun onDock() {
        // at the moment, just create a blank drawing
        domain.contentViewer.contents.setAll(RootDrawing())

        val selectedItemProperty = selectionModel.selectionUnmodifiable

        /*
         * Currently, when an Element is selected on the GEF Canvas, the following things happen:
         * * The currentZOrderPolicy is set to the Element's ZOrderPolicy to be able to shift it around on the Z-Axis
         * * The AttributeFragment is given the Element for editing.
         *
         * Both of these operations are currently not supported for multi-selections.
         */
        selectedItemProperty.addListener { it: ListChangeListener.Change<out IContentPart<out Node>> ->
            if (it.list.size == 1) {
                val selected = it.list.first()

                selected.getAdapter(ZOrderPolicy::class.java)?.let { this.currentZOrderPolicy.value = it }
                        ?: run { this.currentZOrderPolicy.value = null }

                this.attributeFragment.editedElement = selected.content as? GraphicObject<*>

                return@addListener
            }

            this.attributeFragment.editedElement = null
            this.currentZOrderPolicy.value = null
        }

        with(workspace.header) {
            // FIXME: Is there a better way to reorder the buttons
            this.items.clear()

            setUpLoadSaveButtons()

            this += separator()

            setUpUndoButtons()

            this += separator()

            setUpBehaviorButtons()

            this += separator()

            setUpZOrderButtons()

            this += separator()

            setUpDebugButtons()
        }
    }

    private fun setUpLoadSaveButtons() {
        with(workspace.header) {
            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.FOLDER)
                action {
                    val files = chooseFile(
                            "Open system state",
                            arrayOf(FileChooser.ExtensionFilter("CouchEdit JSON", "*.cjson")),
                            FileChooserMode.Single,
                            workspace.currentWindow
                    )

                    try {
                        files.firstOrNull()?.let { this@EditorPane.controller.load(it.absolutePath) }
                    } catch (e: Exception) {
                        errorHandler.uncaughtException(Thread.currentThread(), e)
                    }
                }
                tooltip = Tooltip("Open")
            }

            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.CONTENT_SAVE)
                action {
                    try {
                        this@EditorPane.controller.save()
                    } catch (e: Exception) {
                        errorHandler.uncaughtException(Thread.currentThread(), e)
                    }
                }
                enableWhen(this@EditorPane.controller.saveableProperty)
                tooltip = Tooltip("Save")
            }
            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.CONTENT_SAVE_SETTINGS)
                action {
                    val files = chooseFile(
                            "Save system state",
                            arrayOf(FileChooser.ExtensionFilter("CouchEdit JSON", "*.cjson")),
                            FileChooserMode.Save,
                            workspace.currentWindow
                    )

                    try {
                        files.firstOrNull()?.let { this@EditorPane.controller.saveAs(it.absolutePath) }
                    } catch (e: Exception) {
                        errorHandler.uncaughtException(Thread.currentThread(), e)
                    }
                }
                tooltip = Tooltip("Save as")
            }
        }
    }

    /**
     * Sets up the undo / redo actions to execute their counterparts in the [domain]
     */
    private fun setUpUndoButtons() {
        with(workspace.header) {
            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.UNDO_VARIANT)
                tooltip = Tooltip("Undo")
                action { controller.undo() }
                disableWhen(controller.undoableProperty.not())
            }

            button() {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.REDO_VARIANT)
                tooltip = Tooltip("Redo")
                action { controller.redo() }
                disableWhen(controller.redoableProperty.not())
            }
        }
    }

    private fun setUpBehaviorButtons() {
        with(workspace.header) {
            togglebutton("") {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.AUTO_UPLOAD)
                tooltip = Tooltip("Enable live updates. If disabled, changes will only be sent to the system when the" +
                        " dragging ends")
                selectedProperty().bindBidirectional(controller.publishStagingProperty)
            }
        }
    }

    private fun setUpZOrderButtons() {
        with(workspace.header) {
            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.ARRANGE_SEND_TO_BACK)
                tooltip = Tooltip("Send to back")
                action {
                    currentZOrderPolicy.get()?.let {
                        init(it)
                        it.toBackground()
                        commit(it)
                    }
                }
                disableWhen(currentZOrderPolicy.isNull)
            }

            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.ARRANGE_SEND_BACKWARD)
                tooltip = Tooltip("Send backward (one step)")
                action {
                    currentZOrderPolicy.get()?.let {
                        init(it)
                        it.oneStepToBack()
                        commit(it)
                    }
                }
                disableWhen(currentZOrderPolicy.isNull)
            }

            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.ARRANGE_BRING_FORWARD)

                tooltip = Tooltip("Bring forward (one step)")

                action {
                    currentZOrderPolicy.get()?.let {
                        init(it)
                        it.oneStepToFront()
                        commit(it)
                    }
                }
                disableWhen(currentZOrderPolicy.isNull)
            }

            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.ARRANGE_BRING_TO_FRONT)
                tooltip = Tooltip("Bring to front")
                action {
                    currentZOrderPolicy.get()?.let {
                        init(it)
                        it.toForeground()
                        commit(it)
                    }
                }
                disableWhen(currentZOrderPolicy.isNull)
            }
        }
    }

    private fun setUpDebugButtons() {
        with(workspace.header) {
            button {
                addClass("icon-only")
                graphic = generateView(MaterialDesignIcon.BUG)
                action {
                    openDebugView()
                }
            }
        }
    }

    private fun init(policy: IPolicy) {
        policy.init()
    }

    private fun commit(policy: IPolicy) {
        this.domain.execute(policy.commit(), NullProgressMonitor())
    }

    /**
     * Opens a Window with a [DebugView] in a separate scope. That way, multiple DebugViews can coexist independently.
     */
    private fun openDebugView() {
        val debugScope = Scope()

        val debugView = find(DebugView::class.java, scope = debugScope)

        debugView.openWindow(escapeClosesWindow = false, stageStyle = StageStyle.DECORATED)
    }
}
