package de.uulm.se.couchedit.statecharts.processing.labeling

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.containment.Contains
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.Rectangular
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.common.repository.queries.RelationGraphQueries
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.containment.queries.ContainsQueries
import de.uulm.se.couchedit.processing.graphic.queries.ShapedElementQueries
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateHierarchyElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.ParentOf
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelForHierarchyElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsOrthogonalState
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsStateElement
import de.uulm.se.couchedit.util.extensions.asGenericShape
import de.uulm.se.couchedit.util.extensions.ref

@ProcessorScoped
class StateLabelingProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val relationGraphQueries: RelationGraphQueries,
        private val containsQueries: ContainsQueries,
        private val serviceCaller: ServiceCaller,
        private val shapeExtractor: ShapeExtractor,
        private val shapedElementQueries: ShapedElementQueries
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(
            ShapedElement::class.java,
            Contains::class.java,
            Represents::class.java,
            ParentOf::class.java,
            StateHierarchyElement::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val appliedDiffs = this.applicator.apply(diffs)

        for (diff in appliedDiffs) {
            val affected = diff.affected

            if (affected is ShapedElement<*>) {
                handleShapedElementChange(affected.ref(), ret)
            }

            if (affected is Contains) {
                ret.mergeCollection(this.handleContains(affected))
            }

            if (affected is ParentOf<*, *>) {
                handleParentOfChange(affected, ret)
            }

            if (affected is Represents<*, *> && diff !is ElementRemoveDiff) {
                val containsRelations = modelRepository.getRelationsFromElement(
                        affected.a.id,
                        Contains::class.java,
                        false
                )

                for ((_, relation) in containsRelations) {
                    ret.mergeCollection(handleContains(relation))
                }
            }
        }

        return ret
    }

    /**
     * Reacts to changes in the [Contains] relations between GraphicObjects:
     * If a [Contains] relation towards a Label has changed, check which state representing Elements the Label now
     * has a [Contains] path to. Add and remove the appropriate [LabelForHierarchyElement] relations, then call [updateText] to change
     * the text stored in the State abstract syntax elements.
     */
    private fun handleContains(contains: Contains): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        if (!contains.b.referencesType(GraphicObject::class.java)
                || modelRepository[contains.b.asType<GraphicObject<*>>()]?.shapeClass?.let(Label::class.java::isAssignableFrom) != true) {
            return ret
        }

        // check whether the newly contained element labels a StateElement
        val representedStates = this.containsQueries.mapFirstContainersNotNull(contains.b) {
            // todo also find other labeling styles
            relationGraphQueries.getElementRelatedFrom(it, Represents::class.java, true)
        }

        val currentLabelForRelations = modelRepository.getRelationsFromElement(
                contains.b.id,
                LabelForHierarchyElement::class.java,
                false
        ).values.toMutableSet()

        for (representedState in representedStates) {
            if (currentLabelForRelations.removeIf { it.b == representedState.ref() }) {
                continue
            }

            if (representedState !is StateHierarchyElement) {
                continue
            }

            val probability = calculateStateLabelingProbability(contains.a, representedState.ref(), contains.b)

            val labelFor = LabelForHierarchyElement(
                    contains.b.asType(),
                    representedState.ref(),
                    ProbabilityInfo.Generated(probability)
            )

            ret.mergeCollection(modelRepository.store(labelFor))
        }

        for (oldLabelForRelation in currentLabelForRelations) {
            ret.mergeCollection(modelRepository.remove(oldLabelForRelation.id))
        }

        return ret
    }

    /**
     * As the fact if a StateElement has children is relevant to the calculation of its LabelFor relation's properties,
     * upon deletion / addition of a ParentOf relation we reconsider the outgoing [LabelForHierarchyElement] relations of
     * this label.
     */
    private fun handleParentOfChange(rel: ParentOf<*, *>, diffs: MutableTimedDiffCollection) {
        val stateElementRef = rel.a

        // get all detected labels for the "a" side of the ParentOf relation.
        val labelForRelations = modelRepository.getRelationsToElement(
                stateElementRef.id,
                LabelForHierarchyElement::class.java,
                true
        )

        for (labelForRelation in labelForRelations.values) {
            diffs.mergeCollection(updateLabelForRelation(labelForRelation))
        }
    }

    /**
     * If a [ShapedElement] changes, we need to check all LabelFor relations that Element is either the target of
     * (meaning it by itself labels something) or that go from a [StateHierarchyElement] which is represented by that
     * [ShapedElement].
     *
     * The [updateLabelForRelation] method is then used to calculate the new probability of the labeling based on these
     * [ShapedElement]s' positions.
     */
    private fun handleShapedElementChange(ref: ElementReference<ShapedElement<*>>, diffs: MutableTimedDiffCollection) {
        val allDependent = shapedElementQueries.getDependent(ref)

        val allLabelForRelations = mutableSetOf<LabelForHierarchyElement>()

        for (shapedElement in allDependent) {
            // first, check whether this ShapedElement itself represents something
            val representedElements = modelRepository.getRelationsFromElement(
                    shapedElement.id,
                    Represents::class.java,
                    true
            ).filterValues {
                it is RepresentsStateElement || it is RepresentsOrthogonalState
            }.values.toMutableSet()

            for (represented in representedElements) {
                // (for the case that shapedElement represents a StateElement)
                // Get the LabelFor relations that go from this StateElement

                val labelForRepresented = modelRepository.getRelationsToElement(
                        represented.id,
                        LabelForHierarchyElement::class.java,
                        true
                )

                allLabelForRelations.addAll(labelForRepresented.values)
            }

            // (For the case that shapedElement is a label)
            // Get the labelFor relations that point to the given ShapedElement itself
            val labelForRef = modelRepository.getRelationsFromElement(shapedElement.id, LabelForHierarchyElement::class.java, true)

            allLabelForRelations.addAll(labelForRef.values)
        }

        for (relation in allLabelForRelations) {
            diffs.mergeCollection(updateLabelForRelation(relation))
        }
    }

    /**
     * Reconsiders a [LabelForHierarchyElement] relation, updating its probability.
     */
    private fun updateLabelForRelation(rel: LabelForHierarchyElement): TimedDiffCollection {
        if (rel.probability == ProbabilityInfo.Explicit) {
            return diffCollectionFactory.createMutableTimedDiffCollection()
        }

        val representsRelation = modelRepository.getRelationsToElement(
                rel.b.id,
                Represents::class.java,
                true
        ).values.firstOrNull() ?: return diffCollectionFactory.createMutableTimedDiffCollection()

        val prob = calculateStateLabelingProbability(representsRelation.a, rel.b, rel.a)

        rel.probability = ProbabilityInfo.Generated(prob)

        return modelRepository.store(rel)
    }

    private fun calculateStateLabelingProbability(
            stateHierarchyElementRepresentation: ElementReference<ShapedElement<*>>,
            representedState: ElementReference<StateHierarchyElement>,
            label: ElementReference<ShapedElement<*>>
    ): Double {
        val parentOfRelations = modelRepository.getRelationsFromElement(representedState.id, ParentOf::class.java)

        if (parentOfRelations.isEmpty()) {
            // for atomic states, we always assume that a contained label is the label of the state.
            // This is because there is no real reason to associate the label to a transition in the vincinity.
            return 1.0
        }

        val stateElementReprBBox = serviceCaller.call(
                stateHierarchyElementRepresentation.asGenericShape(),
                shapeExtractor::extractBoundingBox
        ) ?: return 0.1

        val labelShape = serviceCaller.call(
                label.asGenericShape(),
                shapeExtractor::extractShape
        ) as? Rectangular ?: return 0.1

        val xDistance = kotlin.math.abs((labelShape.x - stateElementReprBBox.x) / stateElementReprBBox.w)
        val yDistance = kotlin.math.abs((labelShape.y - stateElementReprBBox.y) / stateElementReprBBox.h)

        // the probability decays a little bit with x distance and a lot with y distance, as composite states
        // and orthogonal regions in Harel's examples are labeled in the top left corner

        val value = 1.0 - (xDistance * xMaxDecay) - (yDistance * yMaxDecay)

        if (value < 0.1) {
            return 0.1
        }
        if (value > 1) {
            return 1.0
        }

        return value
    }

    companion object {
        const val xMaxDecay = 0.3
        const val yMaxDecay = 1.0
    }
}
