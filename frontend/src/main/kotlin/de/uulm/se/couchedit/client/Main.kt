package de.uulm.se.couchedit.client

import de.uulm.se.couchedit.client.view.CouchApplication
import javafx.application.Application

/**
 * Main entry point for the Frontend application. Especially needed when running JFX 10+ as directly running a
 * JavaFX Application object does not seem to work with its gradle plugin.
 */
fun main() {
    Application.launch(CouchApplication::class.java)
}
