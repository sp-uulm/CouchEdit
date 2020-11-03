package de.uulm.se.couchedit.model.graphic.shapes

class Polygon(
        val outerBorder: List<Point>,
        val holes: List<List<Point>> = emptyList()
) : Shape {
    override fun copy(): Polygon {
        return Polygon(
                outerBorder.map(Point::copy),
                holes.map {
                    it.map(Point::copy)
                }
        )
    }
}
