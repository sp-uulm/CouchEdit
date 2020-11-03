package de.uulm.se.couchedit.util.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Calculates a decay function.
 *
 * (([preFactor] * ([input] + [xDisplacement]) ^ [exponent]) + [yDisplacement] in [0.0, 1.0])
 * scaled to [[minValue], [maxValue]]
 *
 * @param exponent Exponent to be applied to the input. For a "real" decay function this must be negative (1/x)
 * @param preFactor The factor to be applied to the input before function calculation
 * @param postFactor The factor to be applied to the function value after function calculation
 * @param xDisplacement Amount to be added or subtracted to / from the input before function application
 * @param yDisplacement Amount to be added or subtracted to / from the input before function application
 * @param minValue Value to which the function output should be scaled if it is over 0.0, if the function output is
 *                 <= 0.0, minValue will be returned
 * @param maxValue Value to which the function output should be scaled if it is under 1.0, if the function output
 *                 is >= 1.0, maxValue will be returned
 */
data class Decay(
        var exponent: Double,
        var preFactor: Double = 1.0,
        var postFactor: Double = 1.0,
        var xDisplacement: Double = 0.0,
        var yDisplacement: Double = 0.0,
        var minValue: Double = 0.0,
        var maxValue: Double = 1.0
) {
    /**
     * Calculates the value of the specified decay function at the given
     */
    fun calculate(input: Double): Double {
        val unscaled = (preFactor * (input + xDisplacement)).pow(exponent)

        return max(min(1.0, (unscaled * postFactor) + yDisplacement), 0.0) * (maxValue - minValue) + minValue
    }
}
