package de.uulm.se.couchedit.export.converters.graphic.shapes

import de.uulm.se.couchedit.export.converters.AbstractConverter
import de.uulm.se.couchedit.export.model.graphic.shapes.SerRectangular
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular

abstract class RectangularConverter<T: Rectangular, S: SerRectangular>: AbstractConverter<T, S>() {
    protected fun <T: SerRectangular> T.setRectangularPropertiesFrom(element: Rectangular): T {
        x = element.x
        y = element.y
        w = element.w
        h = element.h

        return this
    }

    protected fun SerRectangular.getRectangularProperties(): RectangularPropertiesDto {
        return RectangularPropertiesDto(
                x = SerRectangular::x.getNotNull(this),
                y = SerRectangular::y.getNotNull(this),
                w = SerRectangular::w.getNotNull(this),
                h = SerRectangular::h.getNotNull(this)
        )
    }

    protected data class RectangularPropertiesDto(val x: Double, val y: Double, val w: Double, val h: Double)
}
