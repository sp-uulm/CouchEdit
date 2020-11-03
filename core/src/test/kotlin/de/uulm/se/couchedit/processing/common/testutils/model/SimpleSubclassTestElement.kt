package de.uulm.se.couchedit.processing.common.testutils.model

class SimpleSubclassTestElement(id: String, x: String, val z: String) : SimpleTestElement(id, x) {
    override fun copy() = SimpleSubclassTestElement(id, x, z).also { it.probability = this.probability }

    override fun contentEquivalent(other: Any?): Boolean {
        return super.contentEquivalent(other) && (other as SimpleSubclassTestElement).z == this.z
    }
}
