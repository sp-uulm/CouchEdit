package de.uulm.se.couchedit.statecharts.processing.transition

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.hotspots.LineFractionHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.queries.RelationGraphQueries
import de.uulm.se.couchedit.processing.common.services.datastore.IdGenerator
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelFor
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.EndpointFor
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.RepresentsTransition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.TransitionEndPoint
import de.uulm.se.couchedit.statecharts.query.labeling.LabelQueries
import de.uulm.se.couchedit.util.collection.sortedPairOf
import de.uulm.se.couchedit.util.extensions.ref

@ProcessorScoped
class TransitionProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val idGenerator: IdGenerator,
        private val queries: RelationGraphQueries,
        private val labelQueries: LabelQueries
) : Processor {
    override fun consumes() = listOf<Class<out Element>>(
            GraphicObject::class.java,
            LineFractionHotSpotDefinition::class.java,
            ConnectionEnd::class.java,
            StateElement::class.java,
            TransitionEndPoint::class.java,
            LabelFor::class.java,
            RepresentsTransition::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val appliedDiffs = applicator.apply(diffs)

        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val checkedEndPointPairs = mutableSetOf<Pair<String, String>>()

        for (diff in appliedDiffs) {
            when (val affected = diff.affected) {
                is TransitionEndPoint -> {
                    if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                        ret.mergeCollection(checkTransitionEndPoint(affected, checkedEndPointPairs))
                    }
                }
                is EndpointFor -> {
                    // if one endpoint is lost, remove the whole transition (potentially regenerate one later on)
                    if (diff is ElementRemoveDiff) {
                        ret.mergeCollection(modelRepository.remove(affected.b.id))
                    }
                }
                is RepresentsTransition -> {
                    if (diff is ElementRemoveDiff) {
                        ret.mergeCollection(modelRepository.remove(affected.b.id))
                    }
                }
                is LabelFor<*> -> {
                    val affectedElements = labelQueries.getPotentiallyAffectedElements(affected, RepresentsTransition::class.java)

                    val refsToCheck = if(diff is ElementRemoveDiff && affected.b.referencesType(RepresentsTransition::class.java)) {
                        // If the LabelFor Relation has been removed, manually add the affected.b so that the label text
                        // can be removed from it
                        affectedElements + affected.b.asType()
                    } else affectedElements

                    for (ref in refsToCheck) {
                        ret.mergeCollection(updateText(modelRepository[ref] ?: continue))
                    }
                }
                is GraphicObject<*> -> {
                    if (affected.shape is Label) {
                        val refsToCheck = labelQueries.getPotentiallyAffectedElements(affected.ref(), RepresentsTransition::class.java)

                        for (ref in refsToCheck) {
                            ret.mergeCollection(updateText(modelRepository[ref] ?: continue))
                        }
                    }
                }
            }
        }

        return ret
    }

    /**
     * For a new state of a [TransitionEndPoint], inserts / removes the appropriate [Transition] Relations.
     */
    private fun checkTransitionEndPoint(
            endPoint: TransitionEndPoint,
            checkedEndPointPairs: MutableSet<Pair<String, String>>
    ): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        // Retrieve the line which constitutes the endpoint...
        val lineRef = modelRepository[endPoint.a]?.a ?: return ret

        // ... check the other endpoints that this line has.
        val otherEndPoints = modelRepository.getRelationsFromElement(lineRef.id, ConnectionEnd::class.java, false).flatMap { (id, _) ->
            modelRepository.getRelationsFromElement(id, TransitionEndPoint::class.java, false).values
        }

        val explicitEndPoints = otherEndPoints.filter { it.probability == ProbabilityInfo.Explicit }

        if (endPoint.probability !is ProbabilityInfo.Explicit) {
            val sameRoleExplicitEndPoints = explicitEndPoints.filter { it.role == endPoint.role && it.id != endPoint.id }

            if (sameRoleExplicitEndPoints.isNotEmpty()) {
                // If there is a conflicting endpoint with the same role, this endpoint does not get to create a Transition!
                removeAllTransitionsForEndpoint(endPoint, ret)

                checkedEndPointPairs.addAll(otherEndPoints.map { sortedPairOf(endPoint.id, it.id) })

                return ret
            }
        }

        /**
         * If there are potential "peer" endpoints (i.e. with the opposite role) in the otherEndPoints set which have
         * Explicit probability, all other endpoints may not be considered for Transitions. This variable stores this
         * fact.
         */
        val hasOtherRoleExplicitEndPoints = explicitEndPoints.any { it.role != endPoint.role }

        // now try to find a relation for each of these endpoints.
        endPointLoop@ for (otherEndPoint in otherEndPoints) {
            if (otherEndPoint.id == endPoint.id) {
                continue
            }

            val pair = sortedPairOf(endPoint.id, otherEndPoint.id)

            if (pair in checkedEndPointPairs) {
                continue
            }

            checkedEndPointPairs.add(pair)

            /*
             * Opposite case as above: If the checked EndPoint's Probability is Explicit and the other endpoint has the
             * same role but not explicit probability, the other endpoint cannot be part of a transition.
             * Thus, remove all of its defined Transitions.
             */
            if (otherEndPoint.probability != ProbabilityInfo.Explicit
                    && endPoint.probability == ProbabilityInfo.Explicit
                    && endPoint.role == otherEndPoint.role
            ) {
                removeAllTransitionsForEndpoint(otherEndPoint, ret)

                continue@endPointLoop
            }


            if (otherEndPoint.probability != ProbabilityInfo.Explicit && hasOtherRoleExplicitEndPoints) {
                removeAllTransitionsForEndpoint(otherEndPoint, ret)

                continue@endPointLoop
            }

            // find already existing transition element, if possible
            val endPointTransitions = modelRepository
                    .getRelationsFromElement(endPoint.id, EndpointFor::class.java, false)
                    .values
                    .map(EndpointFor::b)

            val otherEndPointTransitions = modelRepository
                    .getRelationsFromElement(otherEndPoint.id, EndpointFor::class.java, false)
                    .values
                    .map(EndpointFor::b)


            // The transition between the two endpoints is the one contained in both Relations from Element
            // sets.
            val commonTransitionRefs = endPointTransitions.intersect(otherEndPointTransitions)

            if (commonTransitionRefs.size > 1) {
                throw IllegalStateException("There may be only one transition between a pair of TransitionEndPoints, " +
                        "got ${commonTransitionRefs.size}")
            }


            var commonTransition = commonTransitionRefs.firstOrNull()?.let {
                modelRepository[it]
            }

            val endPointOrder = getValidTransitionDirection(endPoint, otherEndPoint)

            if (endPointOrder == null) {
                commonTransition?.id?.let {
                    ret.mergeCollection(modelRepository.remove(it))
                }

                continue@endPointLoop
            }

            val (startEndPoint, endEndPoint) = endPointOrder

            val newProbability = calculateTransitionProbability(endPoint.probability, otherEndPoint.probability)

            if (commonTransition != null) {
                commonTransition.probability = newProbability
            } else {
                commonTransition = Transition(
                        idGenerator.generate(Transition::class.java),
                        startEndPoint.b,
                        endEndPoint.b,
                        null,
                        newProbability
                )
            }

            ret.mergeCollection(modelRepository.store(commonTransition))

            val transitionRef = commonTransition.ref()

            ret.mergeCollection(modelRepository.store(EndpointFor(startEndPoint.ref(), transitionRef)))
            ret.mergeCollection(modelRepository.store(EndpointFor(endEndPoint.ref(), transitionRef)))
            ret.mergeCollection(modelRepository.store(RepresentsTransition(lineRef, transitionRef)))

            // add line center HotSpotDefinitíon for label recognition.
            ret.mergeCollection(modelRepository.store(LineFractionHotSpotDefinition(lineRef, 0.5)))
        }

        return ret
    }

    /**
     * Returns the direction in which the Transition between [endPoint1] and [endPoint2] runs.
     *
     * @return Ordered pair of [endPoint1] and [endPoint2] in direction of transition, or null if no valid transition
     *         exists
     *
     * @todo Checking of abstract syntax / semantic correctness; Suggestion on error?
     */
    private fun getValidTransitionDirection(
            endPoint1: TransitionEndPoint,
            endPoint2: TransitionEndPoint
    ): Pair<TransitionEndPoint, TransitionEndPoint>? {
        return if (endPoint1.role == TransitionEndPoint.Role.FROM && endPoint2.role == TransitionEndPoint.Role.TO) {
            Pair(endPoint1, endPoint2)
        } else if (endPoint1.role == TransitionEndPoint.Role.TO && endPoint2.role == TransitionEndPoint.Role.FROM) {
            Pair(endPoint2, endPoint1)
        } else {
            null
        }
    }

    private fun removeAllTransitionsForEndpoint(endPoint: TransitionEndPoint, output: MutableTimedDiffCollection) {
        queries.getElementsRelatedFrom(endPoint.ref(), EndpointFor::class.java, true).forEach {
            output.mergeCollection(modelRepository.remove(it.id))
        }
    }

    /**
     * Calculates the probability of a transition given the probabilities of its EndPoints.
     *
     * @param end1Prob Probability of the first TransitionEndPoint
     * @param end2Prob Probability of the second TransitionEndPoint
     */
    private fun calculateTransitionProbability(end1Prob: ProbabilityInfo?, end2Prob: ProbabilityInfo?): ProbabilityInfo {
        if (end1Prob == ProbabilityInfo.Explicit && end2Prob == ProbabilityInfo.Explicit) {
            return ProbabilityInfo.Explicit
        }

        return ProbabilityInfo.Generated(
                getNumericalTransitionEndProbability(end1Prob) * getNumericalTransitionEndProbability(end2Prob)
        )
    }

    /**
     * Gets the probability of a TransitionEndPoint as a number for when not both ConnectionEnds are Explicit.
     */
    private fun getNumericalTransitionEndProbability(prob: ProbabilityInfo?): Double {
        return when (prob) {
            is ProbabilityInfo.Generated -> prob.probability
            null -> 0.001
            else -> 1.0
        }
    }

    private fun updateText(representsTransition: RepresentsTransition): TimedDiffCollection {
        val transition = modelRepository[representsTransition.b]
                ?: return diffCollectionFactory.createMutableTimedDiffCollection()

        val candidates = labelQueries.getLabelCandidates(representsTransition.ref())

        val text = if(candidates.isEmpty) null else candidates.values().joinToString(" ")

        transition.name = text

        return modelRepository.store(transition)
    }
}
