package de.uulm.se.couchedit.debugui.view.element

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.uulm.se.couchedit.debugui.controller.element.ShapedElementController
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Parent
import javafx.scene.control.Tooltip
import tornadofx.*

class ShapedElementView : View() {
    private val controller: ShapedElementController by inject()

    private val modelRepositoryProp = SimpleObjectProperty<ModelRepository?>()

    private val elementProp = SimpleObjectProperty<ElementReference<ShapedElement<*>>?>()

    var modelRepository: ModelRepository? by modelRepositoryProp

    var shownElement: ElementReference<ShapedElement<*>>? by elementProp

    private val mainGeometryText = textarea(controller.mainGeometryText) {
        isWrapText = true
    }

    private val hotSpotText = textarea(controller.hotSpotsText) {
        isWrapText = true
    }

    private val refreshButton = button("Refresh") {
        graphic = FontAwesomeIconView(FontAwesomeIcon.REFRESH).apply {
            style {
                fill = c("#818181")
            }
            glyphSize = 18
        }
        tooltip = Tooltip("Zoom +")
        action { controller.loadInfo() }
    }

    override val root: Parent = vbox {
        this += refreshButton
        this += mainGeometryText
        this += hotSpotText
    }

    init {
        controller.modelRepository.bind(modelRepositoryProp)
        controller.element.bind(elementProp)
    }
}
