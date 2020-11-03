package de.uulm.se.couchedit.model.graphic.elements

import de.uulm.se.couchedit.model.ElementTest
import de.uulm.se.couchedit.model.graphic.shapes.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1

class PrimitiveGraphicObjectTest : ElementTest<PrimitiveGraphicObject<*>>() {
    override fun getSetsOfMutuallyEquivalentInstances(): Set<Set<PrimitiveGraphicObject<*>>> {
        return setOf(
                setOf(
                        PrimitiveGraphicObject("01", Rectangle(5.0, 5.0, 4.0, 4.0)),
                        PrimitiveGraphicObject("02", Rectangle(5.0, 5.0, 4.0, 4.0)),
                        PrimitiveGraphicObject("03", Rectangle(5.0, 5.0, 4.0, 4.0))
                ),
                setOf(
                        PrimitiveGraphicObject("01", Rectangle(3.0, 5.0, 4.0, 4.0)),
                        PrimitiveGraphicObject("02", Rectangle(3.0, 5.0, 4.0, 4.0)),
                        PrimitiveGraphicObject("03", Rectangle(3.0, 5.0, 4.0, 4.0))
                ),
                setOf(
                        PrimitiveGraphicObject("01", Rectangle(3.0, 5.0, 4.0, 4.0)).apply { z = mutableListOf(1, 2, 3) },
                        PrimitiveGraphicObject("02", Rectangle(3.0, 5.0, 4.0, 4.0)).apply { z = mutableListOf(1, 2, 3) },
                        PrimitiveGraphicObject("03", Rectangle(3.0, 5.0, 4.0, 4.0)).apply { z = mutableListOf(1, 2, 3) }
                ),
                setOf(
                        PrimitiveGraphicObject("01", Label(5.0, 5.0, 4.0, 4.0, "test")),
                        PrimitiveGraphicObject("02", Label(5.0, 5.0, 4.0, 4.0, "test")),
                        PrimitiveGraphicObject("03", Label(5.0, 5.0, 4.0, 4.0, "test"))
                ),
                setOf(
                        PrimitiveGraphicObject("01", Label(5.0, 5.0, 4.0, 4.0, "test2")),
                        PrimitiveGraphicObject("02", Label(5.0, 5.0, 4.0, 4.0, "test2")),
                        PrimitiveGraphicObject("03", Label(5.0, 5.0, 4.0, 4.0, "test2"))
                ),
                setOf(
                        PrimitiveGraphicObject("01", Point(5.0, 3.0)),
                        PrimitiveGraphicObject("02", Point(5.0, 3.0)),
                        PrimitiveGraphicObject("03", Point(5.0, 3.0))
                ),
                setOf(
                        PrimitiveGraphicObject("01", givenAPolygon1()),
                        PrimitiveGraphicObject("02", givenAPolygon1()),
                        PrimitiveGraphicObject("03", givenAPolygon1())
                ),
                setOf(
                        PrimitiveGraphicObject("01", givenAPolygon2()),
                        PrimitiveGraphicObject("02", givenAPolygon2()),
                        PrimitiveGraphicObject("03", givenAPolygon2())
                ),
                setOf(
                        PrimitiveGraphicObject("01", RoundedRectangle(3.0, 5.0, 40.0, 40.0, 10.0)),
                        PrimitiveGraphicObject("02", RoundedRectangle(3.0, 5.0, 40.0, 40.0, 10.0)),
                        PrimitiveGraphicObject("03", RoundedRectangle(3.0, 5.0, 40.0, 40.0, 10.0))
                )
        )
    }

    @Nested
    inner class SetFrom {
        @Suppress("UNCHECKED_CAST")
        @Test
        fun `should make unequivalent instances equivalent`() {
            for ((element1, element2) in contentUnequivalentInstancePairs) {
                val element1Copy = element1.copy()
                val element2Copy = element2.copy()

                element1Copy.setFrom(element2 as GraphicObject<in Shape>)

                assertThat(element1Copy.equivalent(element2))

                element2Copy.setFrom(element1 as GraphicObject<in Shape>)

                assertThat(element2Copy.equivalent(element1))
            }
        }
    }

    override fun getContentRelevantProperties(): Set<KProperty1<in PrimitiveGraphicObject<*>, *>> {
        return setOf(PrimitiveGraphicObject<*>::content, PrimitiveGraphicObject<*>::z)
    }

    private fun givenAPolygon1(): Polygon {
        return Polygon(
                outerBorder = listOf(Point(2.0, 5.0), Point(1.0, 2.0), Point(9.0, 5.0)),
                holes = listOf(listOf(Point(1.0, 2.0), Point(2.0, 3.0), Point(3.0, 4.0)))
        )
    }

    private fun givenAPolygon2(): Polygon {
        return Polygon(
                outerBorder = listOf(Point(3.0, 42.0), Point(9.0, 5.0), Point(13.0, 37.0)),
                holes = listOf(listOf(Point(1.0, 2.0), Point(2.0, 3.0), Point(3.0, 4.0)))
        )
    }
}
