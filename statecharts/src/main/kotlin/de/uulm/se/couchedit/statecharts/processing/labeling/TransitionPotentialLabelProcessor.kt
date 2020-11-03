package de.uulm.se.couchedit.statecharts.processing.labeling

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.hotspots.LineFractionHotSpotDefinition
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Point
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.spatial.services.geometric.NearestPointCalculator
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelFor
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelForTransition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.RepresentsTransition
import de.uulm.se.couchedit.util.extensions.asGenericShape
import de.uulm.se.couchedit.util.extensions.ref
import de.uulm.se.couchedit.util.math.Decay

/**
 * When a transition is detected with the [RepresentsTransition] Relation, this Processor looks for Labels that are
 * near the transition line (and especially its center) and assigns LabelFor relations to the applying labels.
 *
 * The transition's "real" label is then chosen by a separate processor.
 */
@ProcessorScoped
class TransitionPotentialLabelProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val shapeExtractor: ShapeExtractor,
        private val nearestPointCalculator: NearestPointCalculator,
        private val serviceCaller: ServiceCaller
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(
            GraphicObject::class.java,
            LineFractionHotSpotDefinition::class.java,
            StateElement::class.java,
            Transition::class.java,
            RepresentsTransition::class.java,
            LabelFor::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val appliedDiffs = this.applicator.apply(diffs)

        val checkedLineLabelPairs = mutableSetOf<Pair<String, String>>()

        for (diff in appliedDiffs) {
            if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                when (val affected = diff.affected) {
                    is GraphicObject<*> -> {
                        if (Label::class.java.isAssignableFrom(affected.shapeClass)) {
                            // check the label for all transition-representing lines
                            ret.mergeCollection(checkLabelsForLines(
                                    checkedLineLabelPairs,
                                    null,
                                    setOf(affected.ref().asType<GraphicObject<Label>>())
                            ))
                        }

                        if (Line::class.java.isAssignableFrom(affected.shapeClass)) {
                            ret.mergeCollection(checkLabelsForLines(
                                    checkedLineLabelPairs,
                                    setOf(affected.ref().asType<GraphicObject<Line>>()),
                                    null
                            ))
                        }
                    }
                    is RepresentsTransition -> {
                        ret.mergeCollection(checkLabelsForLines(
                                checkedLineLabelPairs,
                                setOf(affected.a.asType()),
                                null
                        ))
                    }
                }
            }
        }

        return ret
    }

    /*
     * Leave the explicit type arguments here because getAllShapedElements() without
     * parameter just looks wrong
     */
    @Suppress("RemoveExplicitTypeArguments")
    private fun checkLabelsForLines(
            checkedLineLabelPairs: MutableSet<Pair<String, String>>,
            givenLines: Set<ElementReference<ShapedElement<Line>>>?,
            givenLabels: Set<ElementReference<ShapedElement<Label>>>?
    ): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val lines = givenLines ?: getAllShapedElements<Line>()
        val labels = givenLabels ?: getAllShapedElements<Label>()

        for (line in lines) {
            for (label in labels) {
                ret.mergeCollection(this.checkLabelForLine(line, label, checkedLineLabelPairs))
            }
        }

        return ret
    }

    /**
     *
     */
    private fun checkLabelForLine(
            line: ElementReference<ShapedElement<Line>>,
            label: ElementReference<ShapedElement<Label>>,
            checkedLineLabelPairs: MutableSet<Pair<String, String>>): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val idPair = Pair(line.id, label.id)

        if (idPair in checkedLineLabelPairs) {
            return ret
        }

        checkedLineLabelPairs.add(idPair)

        // fetch all representsTransition relations for this Line -> we have to generate LabelFor Relations for all of
        // these if the label matches the conditions to be a label for the line
        val representsTransitionRelations = this.modelRepository.getRelationsFromElement(
                line.id,
                RepresentsTransition::class.java,
                true
        )

        // fetch all LabelFor relations that this Label already has
        val existingRelations = this.modelRepository.getRelationsFromElement(
                label.id,
                LabelFor::class.java,
                true
        )

        val existingTransitionLabelingRelations = mutableMapOf<String, LabelForTransition>()
        val explicitLabelingRelations = mutableSetOf<LabelFor<*>>()

        for ((_, relation) in existingRelations) {
            if (relation is LabelForTransition && relation.b.id in representsTransitionRelations) {
                existingTransitionLabelingRelations[relation.b.id] = relation
                continue
            }

            if (relation.probability == ProbabilityInfo.Explicit) {
                // the given label is an explicit label for another element, so remove all other relations and do not
                // calculate new ones.
                explicitLabelingRelations.add(relation)
            }

        }

        if (explicitLabelingRelations.isNotEmpty()) {
            existingTransitionLabelingRelations.values.minus(explicitLabelingRelations).forEach {
                ret.mergeCollection(modelRepository.remove(it.id))
            }

            return ret
        }

        val prob = isLabelForTransition(line, label)

        for ((id, representsTransition) in representsTransitionRelations) {
            var labelingRelation = existingTransitionLabelingRelations[id]

            if (prob >= 0) {
                val probInfo = ProbabilityInfo.Generated(prob)

                if (labelingRelation != null) {
                    labelingRelation.probability = probInfo
                } else {
                    labelingRelation = LabelForTransition(label, representsTransition.ref(), probInfo)
                }

                ret.mergeCollection(modelRepository.store(labelingRelation))
            } else {
                labelingRelation?.let {
                    if (it.probability != ProbabilityInfo.Explicit) {
                        ret.mergeCollection(modelRepository.remove(it.id))
                    }
                }
            }
        }


        return ret
    }

    /**
     * @return Probability that the [label] labels the Transition connected by [representsTransition], or -1.0 if
     *         the conditions are not given.
     */
    private fun isLabelForTransition(
            line: ElementReference<ShapedElement<Line>>,
            label: ElementReference<ShapedElement<Label>>
    ): Double {
        val lineFractionHotSpotDefinition = modelRepository.getRelationsFromElement(
                line.id,
                LineFractionHotSpotDefinition::class.java,
                false
        ).values.find { it.offset == 0.5 } ?: return -1.0

        val transitionLineShape = serviceCaller.call(line, shapeExtractor::extractShape)
                ?: return -1.0
        val labelShape = serviceCaller.call(label, shapeExtractor::extractShape) ?: return -1.0

        val dLine = nearestPointCalculator.calculateDistanceBetween(
                transitionLineShape,
                labelShape,
                line.id,
                label.id
        )

        if (dLine > LINE_DISTANCE_THRESHOLD) {
            return -1.0
        }

        val lineCenterPoint = serviceCaller.call(
                lineFractionHotSpotDefinition.ref().asGenericShape(),
                shapeExtractor::extractShape
        ) as Point

        val dCenter = nearestPointCalculator.calculateDistanceBetween(
                lineCenterPoint,
                labelShape,
                lineFractionHotSpotDefinition.id,
                label.id
        )

        return calculateProbabilityOfConnection(dLine, dCenter)
    }

    private fun calculateProbabilityOfConnection(lineDistance: Double, centerPointDistance: Double): Double {
        val value = (0.9 * lineDistanceDecay.calculate(lineDistance)
                + 0.3 * centerDistanceDecay.calculate(centerPointDistance))

        if (value > 1) {
            return 1.0
        }
        if (value < 0) {
            return 0.0
        }

        return value
    }

    private inline fun <reified T : Shape> getAllShapedElements(): Set<ElementReference<ShapedElement<T>>> {
        return modelRepository.getAllIncludingSubTypes(ShapedElement::class.java).filterValues {
            T::class.java.isAssignableFrom(it.shapeClass)
        }.map {
            @Suppress("UNCHECKED_CAST") // checked by isAssignableFrom
            it.value.ref().asType<ShapedElement<T>>()
        }.toSet()
    }

    companion object {
        const val LINE_DISTANCE_THRESHOLD = 60.0

        private val lineDistanceDecay = Decay(
                exponent = -0.125,
                preFactor = 0.02,
                postFactor = 0.8,
                yDisplacement = -0.2
        )

        private val centerDistanceDecay = Decay(
                exponent = -0.5,
                preFactor = 0.2,
                postFactor = 0.8,
                yDisplacement = -0.2
        )
    }
}
