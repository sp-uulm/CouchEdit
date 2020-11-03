package de.uulm.se.couchedit.model.graphic.shapes

/**
 * A polyline consisting of multiple [points] that are connected by straight lines between them.
 */
class StraightSegmentLine(points: List<Point> = emptyList()) : Line() {
    var points: MutableList<Point> = points.toMutableList()

    /**
     * Count of the [Point]s in this Line.
     */
    val numPoints: Int get() = this.points.size

    /**
     * First point of the line. Distinction between [start] and [end] only is relevant if the line has a semantic
     * direction.
     */
    override var start: Point
        get() = points[0]
        set(value) {
            this.points[0] = value
        }

    /**
     * Last point of the line. Distinction between [start] and [end] only is relevant if the line has a semantic
     * direction.
     */
    override var end: Point
        get() = points[points.size - 1]
        set(value) {
            this.points[points.size - 1] = value
        }

    /**
     * Adds a point before the point with Index [beforeIndex]
     */
    fun addPointBefore(beforeIndex: Int, point: Point) {
        this.points.add(beforeIndex, point)
    }

    /**
     * Adds a point after the point with Index [beforeIndex]
     */
    fun addPointAfter(afterIndex: Int, point: Point) {
        this.points.add(afterIndex + 1, point)
    }

    /**
     * Appends a point at the end of the line. This will become the new [end] point.
     */
    fun addPointToEnd(point: Point) {
        this.points.add(point)
    }

    /**
     * Removes the point stored at index [index]
     *
     * @throws IllegalStateException If trying to remove the last point of the line
     */
    fun removePoint(index: Int) {
        if (this.points.size <= 1) {
            throw IllegalStateException("A line must have at least one point!")
        }

        this.points.removeAt(index)
    }

    /**
     * Returns this Line's [Point] at the given [index].
     */
    operator fun get(index: Int): Point {
        return this.points[index]
    }

    operator fun plus(p: Point) {
        this.addPointToEnd(p)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val that = other as StraightSegmentLine?

        if (this.points.size != that!!.points.size) {
            return false
        }

        for (i in this.points.indices) {
            if (this.points[i] != that.points[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        return points.hashCode()
    }

    override fun copy(): StraightSegmentLine = StraightSegmentLine(points.map(Point::copy))
    override fun toString() = "StraightSegmentLine(" + this.points.map(Point::toString).joinToString(";") + ")"
}
