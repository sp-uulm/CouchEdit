package de.uulm.se.couchedit.client.view.canvas

import de.uulm.se.couchedit.client.util.gef.contentViewer
import org.eclipse.gef.mvc.fx.domain.IDomain
import tornadofx.*

/**
 * TornadoFX View containing the main GEF canvas.
 */
class GefCanvasView : View() {
    /**
     * Domain that the Canvas will operate in
     */
    private val domain: IDomain by di()

    override val root = borderpane {
        minHeight = 300.0
        minWidth = 300.0
    }

    init {
        root.apply {
            this.center = domain.contentViewer.canvas
        }

        /*
         * Activate GEF domain after the viewer has settled in the scene.
         * https://www.eclipse.org/forums/index.php?t=msg&th=1086449&goto=1766725&#msg_1766725
         */
        domain.contentViewer.canvas.sceneProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                domain.activate()
            }
        }
    }
}
