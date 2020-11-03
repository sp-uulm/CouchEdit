package de.uulm.se.couchedit.client.util.fx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import tornadofx.*

fun generateView(icon: FontAwesomeIcon) = FontAwesomeIconView(icon).apply {
    style {
        fill = c("#818181")
    }
    glyphSize = 18
}

fun generateView(icon: MaterialDesignIcon) = MaterialDesignIconView(icon).apply {
    style {
        fill = c("#818181")
    }
    glyphSize = 20
}
