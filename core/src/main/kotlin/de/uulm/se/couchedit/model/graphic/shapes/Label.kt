package de.uulm.se.couchedit.model.graphic.shapes

/**
 * A textual label.
 * Implemented as a special [Rectangle] that is to be rendered within the given [x], [y], [w], [h] bounds and
 * with the given [text] included in it as a text.
 */
class Label(
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double = 0.0,
        h: Double = 0.0,
        var text: String = ""
) : Rectangular(x, y, w, h) {
    override fun copy(): Label = Label(x, y, w, h, text)

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && (other as? Label)?.text == this.text
    }

    override fun hashCode(): Int {
        return 31 * super.hashCode() + text.hashCode()
    }

    override fun toString(): String = "Label(text=\"$text\"x=$x,y=$y,w=$w,h=$h)"
}
