package de.uulm.se.couchedit.statecharts.processing

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.base.suggestions.BaseSuggestion
import de.uulm.se.couchedit.model.base.suggestions.SuggestionFor
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.common.repository.queries.RelationGraphQueries
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateChartAbstractSyntaxElement
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelFor
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.LabelForTransition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsStateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.EndpointFor
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.RepresentsTransition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.TransitionEndPoint
import de.uulm.se.couchedit.util.extensions.ref

@ProcessorScoped
class DisambiguationProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val queries: RelationGraphQueries,
        private val serviceCaller: ServiceCaller,
        private val shapeExtractor: ShapeExtractor
) : Processor {
    override fun consumes() = listOf(
            ShapedElement::class.java,
            StateChartAbstractSyntaxElement::class.java,
            RepresentsTransition::class.java,
            RepresentsStateElement::class.java,
            LabelFor::class.java,
            ConnectionEnd::class.java,
            TransitionEndPoint::class.java,
            EndpointFor::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val appliedDiffs = applicator.apply(diffs)

        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        diffLoop@ for (diff in appliedDiffs) {
            when (val affected = diff.affected) {
                is Transition -> {
                    if (diff is ElementRemoveDiff) {
                        ret.mergeCollection(removeTransitionSuggestion(affected))
                        continue@diffLoop
                    }

                    ret.mergeCollection(generateTransitionSuggestion(affected))
                }

                is StateElement -> {
                    // upon change of state labels, we potentially need to update suggestion texts.
                    val suggestions = queries.getElementsRelatedTo(affected.ref(), ExplicitTransitionSuggestionDependsOn::class.java, true)

                    for (suggestion in suggestions) {
                        ret.mergeCollection(updateTitleOfTransitionSuggestion(suggestion))
                    }
                }

                is RepresentsTransition -> {
                    ret.mergeCollection(generateTransitionSuggestion(modelRepository[affected.b] ?: continue@diffLoop))
                }

                is LabelFor<*> -> {
                    if (diff is ElementRemoveDiff) {
                        // If an Explicit LabelFor relation is removed, need to check all other LabelFor relations
                        // for this label as now a new possibility may arise again
                        if (affected.probability is ProbabilityInfo.Explicit) {
                            val labelForRelations = getAllLabelForRelations(affected.a.id).values

                            for (relation in labelForRelations) {
                                ret.mergeCollection(generateLabelForSuggestion(relation))
                            }
                        }

                        ret.mergeCollection(removeLabelForSuggestion(affected))

                        continue@diffLoop
                    }

                    ret.mergeCollection(generateLabelForSuggestion(affected))
                }
            }
        }

        return ret
    }

    private fun generateTransitionSuggestion(transition: Transition): TimedDiffCollection {
        if (transition.probability == ProbabilityInfo.Explicit) {
            return removeTransitionSuggestion(transition)
        }

        val endpointForRelations = modelRepository.getRelationsToElement(transition.id, EndpointFor::class.java, true).values

        if (endpointForRelations.size != 2) {
            return removeTransitionSuggestion(transition)
        }

        val action = diffCollectionFactory.createPreparedDiffCollection()

        for (relation in endpointForRelations) {
            val endpoint = modelRepository[relation.a] ?: continue

            val connectionEnd = modelRepository[endpoint.a] ?: continue

            if (connectionEnd.probability == ProbabilityInfo.Explicit) {
                continue
            }

            val connectionEndCopy = connectionEnd.copy()

            connectionEndCopy.probability = ProbabilityInfo.Explicit

            action.putDiff(ElementModifyDiff(connectionEnd, connectionEndCopy))
        }

        val representingGraphicObjectRef = queries.getElementRelatedTo(
                transition.ref(),
                RepresentsTransition::class.java,
                true
        ) ?: return diffCollectionFactory.createTimedDiffCollection()

        val state1 = modelRepository[transition.a] ?: return removeTransitionSuggestion(transition)
        val state2 = modelRepository[transition.b] ?: return removeTransitionSuggestion(transition)

        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val suggestion = BaseSuggestion(
                generateTransitionSuggestionId(transition),
                generateTitleOfTransitionSuggestion(state1, state2),
                action
        )

        ret.mergeCollection(modelRepository.store(suggestion))

        val suggestionFor = SuggestionFor(suggestion.ref(), representingGraphicObjectRef.ref())

        ret.mergeCollection(modelRepository.store(suggestionFor))

        // These Relations are for internal use only at the moment
        val suggestionDependsOn1 = ExplicitTransitionSuggestionDependsOn(suggestion.ref(), state1.ref())
        modelRepository.store(suggestionDependsOn1)

        val suggestionDependsOn2 = ExplicitTransitionSuggestionDependsOn(suggestion.ref(), state2.ref())
        modelRepository.store(suggestionDependsOn2)

        val suggestionPertainsTo = ExplicitTransitionSuggestionPertainsTo(suggestion.ref(), transition.ref())
        modelRepository.store(suggestionPertainsTo)

        return ret
    }

    private fun updateTitleOfTransitionSuggestion(suggestion: BaseSuggestion): TimedDiffCollection {
        val pertainingTransition = queries.getElementRelatedFrom(
                suggestion.ref(),
                ExplicitTransitionSuggestionPertainsTo::class.java,
                true
        ) ?: return modelRepository.remove(suggestion.id)

        val state1 = modelRepository[pertainingTransition.a]
        val state2 = modelRepository[pertainingTransition.b]

        if (state1 == null || state2 == null) {
            return modelRepository.remove(suggestion.id)
        }

        suggestion.title = generateTitleOfTransitionSuggestion(state1, state2)

        return modelRepository.store(suggestion)
    }

    private fun generateTitleOfTransitionSuggestion(state1: StateElement, state2: StateElement): String {
        return "Explicitly define transition from " +
                "\"${state1.name ?: "<unnamed>"}\" " +
                "to \"${state2.name ?: "<unnamed>"}\""
    }

    private fun removeTransitionSuggestion(transition: Transition): TimedDiffCollection {
        val id = generateTransitionSuggestionId(transition)

        return modelRepository.remove(id)
    }

    private fun generateTransitionSuggestionId(transition: Transition): String {
        return "Suggest_Expl_T_${transition.id}"
    }

    private fun generateLabelForSuggestion(labelFor: LabelFor<*>): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        /*
         * For determining whether or not we should generate a Suggestion for this LabelFor relation
         * we need to consider the other LabelFor relations that go from this Label ShapedElement.
         */
        val labelForRelations = getAllLabelForRelations(labelFor.a.id)

        if (labelForRelations.any { (_, rel) -> rel.probability == ProbabilityInfo.Explicit }) {
            // This label already is explicitly assigned to an abstract syntax Element as a label. Remove all suggestions

            val currentSuggestions = modelRepository.getRelationsToElement(
                    labelFor.a.id,
                    LabelForSuggestionDependsOn::class.java,
                    false
            )

            for (dependsOn in currentSuggestions.values) {
                ret.mergeCollection(modelRepository.remove(dependsOn.a.id))
            }

            return ret
        }

        val representingElement = getRepresentingElement(labelFor) ?: return removeLabelForSuggestion(labelFor)

        val label = serviceCaller.call(labelFor.a, shapeExtractor::extractShape)
                ?: return removeLabelForSuggestion(labelFor)

        // if no Explicit relation is there, then suggest converting the labelFor relation into one.

        val action = diffCollectionFactory.createPreparedDiffCollection()

        val newLabelFor = labelFor.copy()
        newLabelFor.probability = ProbabilityInfo.Explicit

        action.putDiff(ElementModifyDiff(labelFor, newLabelFor))

        val suggestion = BaseSuggestion(
                generateLabelForSuggestionId(labelFor),
                generateTitleOfLabelForSuggestion(label, labelFor.b),
                action
        )
        ret.mergeCollection(modelRepository.store(suggestion))

        val suggestionFor = SuggestionFor(suggestion.ref(), representingElement.ref())
        ret.mergeCollection(modelRepository.store(suggestionFor))

        val dependsOn = LabelForSuggestionDependsOn(suggestion.ref(), labelFor.a)
        modelRepository.store(dependsOn)

        return ret
    }

    private fun generateTitleOfLabelForSuggestion(label: Label, target: ElementReference<*>): String {
        val typeString = if (target.referencesType(RepresentsTransition::class.java)) "Transition" else target.type.simpleName

        return "Explicitly label this $typeString as ${label.text}"
    }

    private fun removeLabelForSuggestion(labelFor: LabelFor<*>): TimedDiffCollection {
        val id = generateLabelForSuggestionId(labelFor)

        return modelRepository.remove(id)
    }

    private fun generateLabelForSuggestionId(labelFor: LabelFor<*>): String {
        return "Suggest_Expl_LF_${labelFor.id}"
    }

    private fun getRepresentingElement(labelFor: LabelFor<*>): GraphicObject<*>? {
        if (labelFor is LabelForTransition) {
            val representsTransition = modelRepository[labelFor.b] ?: return null

            return modelRepository[representsTransition.a]
        }

        val represents = modelRepository.getRelationsToElement(
                labelFor.b.id,
                Represents::class.java,
                true
        ).values.firstOrNull()

        return modelRepository[represents?.a] as? GraphicObject<*>
    }

    private fun getAllLabelForRelations(labelId: String): ElementQueryResult<LabelFor<*>> {
        return modelRepository.getRelationsFromElement(labelId, LabelFor::class.java, true)
    }

    private class ExplicitTransitionSuggestionDependsOn(
            a: ElementReference<BaseSuggestion>,
            b: ElementReference<StateElement>
    ) : OneToOneRelation<BaseSuggestion, StateElement>(a, b) {
        override val isDirected: Boolean = true

        override fun copy(): OneToOneRelation<BaseSuggestion, StateElement> = ExplicitTransitionSuggestionDependsOn(a, b)

        override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
    }

    private class ExplicitTransitionSuggestionPertainsTo(
            a: ElementReference<BaseSuggestion>,
            b: ElementReference<Transition>
    ) : OneToOneRelation<BaseSuggestion, Transition>(a, b) {
        override val isDirected: Boolean = true

        override fun copy(): OneToOneRelation<BaseSuggestion, Transition> = ExplicitTransitionSuggestionPertainsTo(a, b)

        override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
    }

    private class LabelForSuggestionDependsOn(
            a: ElementReference<BaseSuggestion>,
            b: ElementReference<ShapedElement<*>>
    ) : OneToOneRelation<BaseSuggestion, ShapedElement<*>>(a, b) {
        override val isDirected: Boolean = true

        override fun copy(): OneToOneRelation<BaseSuggestion, ShapedElement<*>> {
            return LabelForSuggestionDependsOn(a, b)
        }

        override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
    }
}
