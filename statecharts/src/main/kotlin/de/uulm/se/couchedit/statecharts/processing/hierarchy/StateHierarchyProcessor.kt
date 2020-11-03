package de.uulm.se.couchedit.statecharts.processing.hierarchy

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.containment.Contains
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Label
import de.uulm.se.couchedit.model.graphic.shapes.RoundedRectangle
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
import de.uulm.se.couchedit.statecharts.model.couch.elements.*
import de.uulm.se.couchedit.statecharts.model.couch.relations.ContainsRegion
import de.uulm.se.couchedit.statecharts.model.couch.relations.ParentOf
import de.uulm.se.couchedit.statecharts.model.couch.relations.Transition
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.*
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.RepresentsTransition
import de.uulm.se.couchedit.statecharts.query.labeling.LabelQueries
import de.uulm.se.couchedit.util.extensions.ref

/**
 * [Processor] handling the basic adding and removing of [StateElement]s based on [GraphicObject]s
 * and their hierarchy in the form of [ParentOf] relations based on [Contains] relations.
 */
@ProcessorScoped
class StateHierarchyProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val idGenerator: IdGenerator,
        private val queries: RelationGraphQueries,
        private val labelQueries: LabelQueries
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(
            GraphicObject::class.java,
            Contains::class.java,
            CompartmentHotSpotDefinition::class.java,
            StateElement::class.java,
            StateContainer::class.java,
            ContainsRegion::class.java,
            RepresentsOrthogonalState::class.java,
            LabelFor::class.java,
            RepresentsTransition::class.java,
            Transition::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val appliedDiffs = applicator.apply(diffs)

        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        diffLoop@ for (diff in appliedDiffs) {
            when (val affected = diff.affected) {
                is GraphicObject<*> -> {
                    if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                        if (Label::class.java.isAssignableFrom(affected.shapeClass)
                                && diff !is ElementRemoveDiff) {
                            val refsToCheck = labelQueries.getPotentiallyAffectedElements(
                                    affected.ref(),
                                    StateChartAbstractSyntaxElement::class.java
                            )

                            for (ref in refsToCheck) {
                                ret.mergeCollection(updateText(ref))
                            }

                            continue@diffLoop
                        }

                        val representationChanges = checkStateFor(affected)

                        ret.mergeCollection(representationChanges)
                    }

                }
                is Contains -> {
                    ret.mergeCollection(checkContains(affected, diff is ElementRemoveDiff))
                }
                is Represents<*, *> -> {
                    ret.putDiff(diff, appliedDiffs.getVersionForElement(affected.id))

                    ret.mergeCollection(checkParent(affected))

                    if (diff is ElementRemoveDiff) {
                        ret.mergeCollection(this.modelRepository.remove(affected.b.id))
                    }
                }
                is LabelFor<*> -> {
                    val affectedElements = labelQueries.getPotentiallyAffectedElements(affected, StateChartAbstractSyntaxElement::class.java)

                    // if a label no longer belongs to a state, e.g. because the label has been removed,
                    // update its text (so that the text without this particular label is generated).
                    val refsToCheck = if (diff is ElementRemoveDiff
                            && affected.b.referencesType(StateChartAbstractSyntaxElement::class.java)) {
                        affectedElements + affected.b.asType()
                    } else affectedElements

                    for (ref in refsToCheck) {
                        ret.mergeCollection(updateText(ref))
                    }
                }
            }
        }

        return ret
    }

    /**
     * Checks whether this GraphicObject comprises a StateElement and if necessary, generates the necessary Elements
     * (including the Represents relation)
     */
    private fun checkStateFor(go: GraphicObject<*>): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val goRef = go.ref()

        val representsRelations = modelRepository.getRelationsFromElement(go.id, RepresentsStateElement::class.java, true)

        if (representsRelations.size > 1) {
            throw IllegalStateException(
                    "There are ${representsRelations.size} Represents relations from GraphicObject $goRef, expected 0 or 1"
            )
        }

        if (representsRelations.isEmpty()) {
            // GraphicObject is not yet associated to a state representation! Create a new one
            if (RoundedRectangle::class.java.isAssignableFrom(go.shapeClass)) {

                val state = State(idGenerator.generate(State::class.java))
                val represents = RepresentsStateElement(goRef, state.ref())

                ret.mergeCollection(this.modelRepository.store(state))
                ret.mergeCollection(this.modelRepository.store(represents))

                ret.mergeCollection(this.checkParent(represents))

                return ret
            }
        }

        return ret
    }

    /**
     * Checks whether the [StateElement] of the given [Represents] relation has to be contained in a [StateContainer]
     * or, if the [StateElement] is a [StateContainer] itself, checks the Relations that are contained in it
     */
    private fun checkParent(represents: Represents<*, *>): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val representingElementRef = represents.a
        val representedElementRef = represents.b

        if (representedElementRef.referencesType(StateElement::class.java)) {
            // first, get the representing GraphicObject's immediate parent element
            val containsRelationsTo = modelRepository.getRelationsToElement(representingElementRef.id, Contains::class.java, false).values

            for (relation in containsRelationsTo) {
                ret.mergeCollection(checkContains(relation, false, null, representedElementRef.asType()))
            }
        }

        if (representedElementRef.referencesType(StateContainer::class.java)) {
            // get all elements that are contained in the representing GraphicObject
            val containsRelationsFrom = modelRepository.getRelationsFromElement(representingElementRef.id, Contains::class.java, false).values

            for (relation in containsRelationsFrom) {
                ret.mergeCollection(checkContains(relation, false, representedElementRef, null))
            }
        }

        return ret
    }

    /**
     * Checks whether the given [contains] relation between two GraphicObjects also represents a
     * [ParentOf] relation between the States represented by the GraphicObjects.
     * If yes, depending on the value of [wasRemoved], a [ParentOf] relation is either stored into or removed from this
     * Processor's [modelRepository].
     */
    private fun checkContains(
            contains: Contains,
            wasRemoved: Boolean,
            knownRepresentedA: ElementReference<StateChartAbstractSyntaxElement>? = null,
            knownRepresentedB: ElementReference<StateElement>? = null
    ): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        /*
         * If one of the elements has been removed, the represents relation and thus the state will be
         * removed with it, thus no need to remove it manually.
         */
        if (modelRepository[contains.a] == null || modelRepository[contains.b] == null) {
            return ret
        }

        val representedA = knownRepresentedA
                ?: this.queries.getElementsRelatedFrom(contains.a, Represents::class.java, true)
                        .filterIsInstance<StateHierarchyElement>().singleOrNull()?.ref() ?: return ret
        val representedB = knownRepresentedB
                ?: this.queries.getElementsRelatedFrom(contains.b, Represents::class.java, true)
                        .filterIsInstance<StateHierarchyElement>().singleOrNull()?.ref() ?: return ret

        if (wasRemoved) {
            val parentOfRelations = modelRepository.getRelationsBetweenElements(
                    representedA.id,
                    representedB.id,
                    ParentOf::class.java,
                    true
            )

            for ((id, _) in parentOfRelations) {
                ret.mergeCollection(modelRepository.remove(id))
            }
        } else {
            if (representedA.referencesType(StateContainer::class.java) && representedB.referencesType(StateElement::class.java)) {
                val parentOfRelation = ParentOf(representedA.asType<StateContainer>(), representedB.asType<StateElement>())

                ret.mergeCollection(this.modelRepository.store(parentOfRelation))
            }
        }

        return ret
    }

    /**
     * Updates the Name of a [StateChartAbstractSyntaxElement] based on its [LabelForHierarchyElement] Relations
     */
    private fun updateText(ref: ElementReference<StateChartAbstractSyntaxElement>): MutableTimedDiffCollection {
        return if (ref.referencesType(StateHierarchyElement::class.java)) {
            this.updateStateText(ref.asType())
        } else {
            diffCollectionFactory.createMutableTimedDiffCollection()
        }
    }

    /**
     * Updates the Name of a [StateElement] based on its [LabelForHierarchyElement] Relations
     */
    private fun updateStateText(stateRef: ElementReference<StateHierarchyElement>): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val state = modelRepository[stateRef] ?: return ret

        val texts = labelQueries.getLabelCandidates(stateRef)

        // TODO: Differentiate texts?
        state.name = if (texts.isEmpty()) null else texts.values().joinToString(" ")

        ret.mergeCollection(modelRepository.store(state))

        return ret
    }
}
