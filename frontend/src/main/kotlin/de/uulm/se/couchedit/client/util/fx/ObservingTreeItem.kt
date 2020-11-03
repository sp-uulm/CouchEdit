package de.uulm.se.couchedit.client.util.fx

import com.google.common.base.Objects
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.TreeItem
import tornadofx.*

/**
 * [TreeItem] that automatically updates its children backed by the values in an [ObservableMap].
 *
 * @param value The value to be displayed in this [TreeItem]
 * @param graphic The graphic to be associated with this [TreeItem]. Sub-items won't get a graphic.
 * @param childrenRetriever Function fetching a ObservableMap of other [T] elements associated with this TreeItem.
 *                          The values of the map will be observed and the children automatically updated accordingly.
 */
class ObservingTreeItem<T>(
        value: T? = null,
        graphic: Node? = null,
        val childrenRetriever: (T) -> ObservableMap<out Any, out T>
) : TreeItem<T>(value, graphic) {
    init {
        this.observeChildren()
    }

    private fun observeChildren() {
        if (value == null) {
            return
        }

        val childrenMap = childrenRetriever(value)

        this.children.bind(childrenMap) { _, value ->
            return@bind ObservingTreeItem(value, null, childrenRetriever)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObservingTreeItem<*>

        if (value != other.value) return false
        if (graphic != other.graphic) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this::class.java, value, graphic)
    }
}
