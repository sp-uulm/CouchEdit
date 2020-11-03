package de.uulm.se.couchedit.client.util.fx

import com.google.common.base.Objects
import javafx.scene.Node
import javafx.scene.control.MenuItem

/**
 * A [MenuItem] where the equality is purely decided based on the [id].
 */
class IdEqualsMenuItem(text: String? = null, graphic: Node? = null) : MenuItem(text, graphic) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return this.id == (other as MenuItem).id
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this::class.java, id)
    }
}
