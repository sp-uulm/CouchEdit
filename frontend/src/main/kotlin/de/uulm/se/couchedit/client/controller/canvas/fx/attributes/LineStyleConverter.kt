package de.uulm.se.couchedit.client.controller.canvas.fx.attributes

import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineEndPointStyle
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Polygon
import javafx.scene.shape.Shape

/**
 * Converts [Attribute]s pertaining to line properties into the appropriate JavaFX objects.
 */
class LineStyleConverter {
    fun getFXShapeForEndPointStyle(attr: LineEndPointStyle): Shape? {
        return when(attr.value) {
            LineEndPointStyle.Option.NONE -> null
            LineEndPointStyle.Option.SOLIDARROW -> {
                val deco = Polygon(0.0, 0.0, 10.0, 3.0, 10.0, -3.0)

                deco.fill = Color.BLACK

                return deco
            }

        }
    }

    /**
     * Returns a JavaFX Stroke-Dash list that can be input to the [javafx.scene.shape.Line.getStrokeDashArray] property
     * in order to draw a dashed line.
     */
    fun getFXStrokeDashStyleForLineStyle(attr: LineStyle): List<Double> {
        return when(attr.value) {
            LineStyle.Option.SOLID -> emptyList()
            LineStyle.Option.DASHED -> listOf(5.0, 3.0)
        }
    }
}
