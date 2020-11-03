package de.uulm.se.couchedit.debugui.util.fx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import tornadofx.*

/*
 * This is copied from the frontend module to maintain decoupling. Potentially make own module with utility
 * functions in the future?
 */
fun generateView(icon: FontAwesomeIcon) = FontAwesomeIconView(icon).apply {
    style {
        fill = c("#818181")
    }
    glyphSize = 18
}
