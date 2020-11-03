package de.uulm.se.couchedit.client.style

import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import tornadofx.*

class InPlaceEditStyleSheet : Stylesheet() {
    companion object {
        val inPlaceEdit by cssclass()
    }

    init {
        label {
            and(inPlaceEdit) {
                textAlignment = TextAlignment.CENTER
                alignment = Pos.CENTER
                wrapText = true
            }
        }

        textArea {
            and(inPlaceEdit) {
                backgroundColor = multi(c(20, 160, 240, 0.6))

                wrapText = true
                text {
                    textAlignment = TextAlignment.CENTER
                }
                scrollPane {
                    // Hide scroll bars in inline editing blocks - they block the limited space and cannot be used
                    // correctly anyway because they are in conflict with the translate on drag system
                    hBarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                    vBarPolicy = ScrollPane.ScrollBarPolicy.NEVER

                    backgroundColor = multi(Color.TRANSPARENT)

                    viewport {
                        backgroundColor = multi(Color.TRANSPARENT)
                    }
                    content {
                        backgroundColor = multi(Color.TRANSPARENT)
                    }
                }
            }
        }
    }
}
