package de.uulm.se.couchedit.statecharts.query.labeling

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelFor

@ProcessorScoped
class LabelQueries @Inject constructor(
        private val modelRepository: ModelRepository,
        private val shapeExtractor: ShapeExtractor,
        private val serviceCaller: ServiceCaller
) {
    /**
     * Gets a mapping from probability of a labeling to the label's contained text for a given
     * [StateChartAbstractSyntaxElement] or RepresentsTransition relation.
     *
     * @return Mapping probability -> text of LabelFor relations that match the criteria of this query object.
     *         Probability = 2.0 means Explicit probability.
     */
    fun getLabelCandidates(elRef: ElementReference<*>): Multimap<Double, String> {
        val ret = HashMultimap.create<Double, String>()

        // first, get all labels that have a LabelFor relation towards this Element
        val labelForRelations = modelRepository.getRelationsToElement(elRef.id, LabelFor::class.java, true).toMutableMap()

        val explicitLabelForRels = labelForRelations.values.filter { it.probability == ProbabilityInfo.Explicit }

        for (labelForRel in explicitLabelForRels) {
            val shape = serviceCaller.call(labelForRel.a, shapeExtractor::extractShape) ?: continue

            ret.put(2.0, shape.text)

            labelForRelations.remove(labelForRel.id)
        }

        // now check all alternatively labeled elements for this candidate.
        for (candidate in labelForRelations.values) {
            val candProb = (candidate.probability as ProbabilityInfo.Generated).probability

            val otherLabelForRelations = modelRepository.getRelationsFromElement(candidate.a.id, LabelFor::class.java, true).minus(candidate.id)

            val maxProbableOther = otherLabelForRelations.maxBy { (_, rel) ->
                (rel.probability as? ProbabilityInfo.Generated)?.probability ?: Double.MAX_VALUE
            }?.value

            if (maxProbableOther?.probability == ProbabilityInfo.Explicit) {
                // The candidate can never win against Explicit probability.
                continue
            }

            if (((maxProbableOther?.probability as? ProbabilityInfo.Generated)?.probability
                            ?: Double.MIN_VALUE) < candProb - MIN_PROBABILITY_DIFFERENCE) {
                ret.put(candProb, getLabelFromLabelForRelation(candidate))
            }
        }

        return ret
    }

    fun <T : Element> getPotentiallyAffectedElements(labelFor: LabelFor<*>, elementType: Class<T>): Set<ElementReference<T>> {
        return getPotentiallyAffectedElements(labelFor.a, elementType)
    }

    /**
     * If a [LabelFor] relation changes, there could be more Elements affected than just the direct endpoint of the
     * given [labelFor] object (as Processors determining Element labels must also take into account the other LabelFor
     * relations in the vicinity).
     *
     * This method finds all of the Elements of a given [elementType] (and its subtypes) that must be reconsidered.
     */
    fun <T : Element> getPotentiallyAffectedElements(
            label: ElementReference<ShapedElement<*>>,
            elementType: Class<T>
    ): Set<ElementReference<T>> {
        val labelForRelations = modelRepository.getRelationsFromElement(label.id, LabelFor::class.java, true).values

        return labelForRelations.mapNotNull {
            if (it.b.referencesType(elementType)) {
                @Suppress("UNCHECKED_CAST") // checked by ReferencesType
                return@mapNotNull it.b as ElementReference<T>
            }
            return@mapNotNull null
        }.toSet()
    }

    private fun getLabelFromLabelForRelation(rel: LabelFor<*>): String? {
        val shape = serviceCaller.call(rel.a, shapeExtractor::extractShape)

        return shape?.text
    }

    companion object {
        /**
         * The amount by which the LabelFor relation for a label ShapedElement must surpass all other LabelFor
         * relations towards other elements to be considered a candidate.
         */
        const val MIN_PROBABILITY_DIFFERENCE = 0.2
    }
}
