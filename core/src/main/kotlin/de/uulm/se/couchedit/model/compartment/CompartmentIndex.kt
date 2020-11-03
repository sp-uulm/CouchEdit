package de.uulm.se.couchedit.model.compartment

/**
 * Uniquely identifies a [CompartmentHotSpotDefinition] or a [PotentialCompartment] among a set of [CompartmentElement]s
 * with the same relation endpoints.
 *
 * To catch all edge cases, Compartments are identified by:
 * 1. The upper left corner of their bounding box ([indexUL])
 * 2. The lower right corner of their bounding box ([indexBR])
 * 3. An arbitrary point inside the interior of the compartment area ([interiorPoint])
 *
 * These possibilities are only evaluated until noo ambiguity exists. For the rare case that the bounding boxes are
 * identical, [interiorPoint] always uniquely identifies the compartment as compartments
 * (dependent on the same Elements) are disjoint by definition.
 */
data class CompartmentIndex(
        val indexUL: Pair<Int, Int>,
        val indexBR: Pair<Int, Int>? = null,
        val interiorPoint: Pair<Double, Double>? = null
) {
    init {
        require(interiorPoint == null || indexBR != null) {
            "CompartmentIndex cannot have interiorPoint if indexBR is null"
        }
    }

    private val strRepr: String by lazy {
        var ret = pairToString(indexUL)

        if (indexBR != null) {
            ret += pairToString(indexBR)
        }

        if (interiorPoint != null) {
            ret += pairToString(interiorPoint)
        }

        return@lazy ret
    }

    override fun toString() = strRepr

    companion object {
        private fun pairToString(pair: Pair<Number, Number>): String {
            return "(${pair.first},${pair.second})"
        }
    }
}
