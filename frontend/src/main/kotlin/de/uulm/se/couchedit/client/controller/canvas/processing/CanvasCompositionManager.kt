package de.uulm.se.couchedit.client.controller.canvas.processing

import com.google.inject.Inject
import de.uulm.se.couchedit.client.controller.canvas.gef.part.BasePart
import de.uulm.se.couchedit.client.controller.canvas.gef.part.system.RootDrawingPart
import de.uulm.se.couchedit.client.util.gef.contentViewer
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.subjects.ReplaySubject
import org.eclipse.gef.mvc.fx.domain.IDomain
import org.eclipse.gef.mvc.fx.parts.AbstractContentPart

internal class CanvasCompositionManager @Inject constructor(private val domain: IDomain) {
    internal val elementUpdateSubject = ReplaySubject.create<Triple<BasePart<*, *>, BasePart<*, *>, ModelDiff>>()

    init {
        this.elementUpdateSubject.observeOn(JavaFxScheduler.platform()).subscribe { (includedPart, compositePart, diff) ->
            this.onIncomingDiff(includedPart, compositePart, diff)
        }
    }

    private fun onIncomingDiff(includedPart: BasePart<*, *>, compositePart: BasePart<*, *>, diff: ModelDiff) {
        if (diff is ElementAddDiff) {
            if (includedPart.getParent() === compositePart) {
                return
            }

            val content = includedPart.getContent()

            val anchoreds = includedPart.getAnchoredsUnmodifiable().filterIsInstance(BasePart::class.java)

            val parentPart = includedPart.getParent()

            val anchoredsWithRoles = mutableListOf<Pair<BasePart<*, *>, String>>()

            if (parentPart !== null) {
                for (anchored in anchoreds) {
                    val roles = anchored.getContentAnchoragesUnmodifiable().get(content)

                    for (role in roles) {
                        anchored.detachFromContentAnchorage(content, role)

                        anchoredsWithRoles.add(Pair(anchored, role))
                    }
                }

                if (parentPart is AbstractContentPart) {
                    parentPart.removeContentChild(content)
                } else {
                    parentPart.removeChild(includedPart)
                }
            }

            compositePart.addContentChild(content, compositePart.getContentChildrenUnmodifiable().size)

            for ((anchored, role) in anchoredsWithRoles) {
                anchored.attachToContentAnchorage(content, role)
            }
        }

        if (diff is ElementRemoveDiff) {
            if (compositePart.getChildrenUnmodifiable().contains(includedPart)) {
                compositePart.removeChild(includedPart)
            }

            getRootDrawingPart().addChild(includedPart)
        }
    }

    private fun getRootDrawingPart() = domain.contentViewer.rootPart.childrenUnmodifiable[0] as RootDrawingPart
}
