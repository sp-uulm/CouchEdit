package de.uulm.se.couchedit.client.view.workspace

import tornadofx.*

/**
 * The root workspace for the application, acting as a container for the [EditorPane], managing menus etc.
 */
class CouchWorkspace : Workspace() {
    override fun onBeforeShow() {
        this.dock<EditorPane>()
    }
}
