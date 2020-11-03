package de.uulm.se.couchedit.client.util.gef

import de.uulm.se.couchedit.client.controller.canvas.gef.part.system.RootDrawingPart
import org.eclipse.gef.mvc.fx.viewer.IViewer

val IViewer.rootDrawingPart: RootDrawingPart
    get() = this.rootPart.childrenUnmodifiable.getOrNull(0) as RootDrawingPart
