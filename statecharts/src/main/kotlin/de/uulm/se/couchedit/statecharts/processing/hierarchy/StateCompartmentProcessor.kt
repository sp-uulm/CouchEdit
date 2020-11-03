package de.uulm.se.couchedit.statecharts.processing.hierarchy

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.relations.OneToOneRelation
import de.uulm.se.couchedit.model.compartment.CompartmentElement
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.compartment.PotentialCompartment
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
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
import de.uulm.se.couchedit.statecharts.model.couch.elements.OrthogonalRegion
import de.uulm.se.couchedit.statecharts.model.couch.elements.State
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.ContainsRegion
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.Represents
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsOrthogonalState
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsStateElement
import de.uulm.se.couchedit.util.extensions.ref

/**
 * Inserts appropriate [OrthogonalRegion] Elements for compartments with dashed lines that are created in StateElements.
 *
 * This processor, for every [CompartmentElement], checks if:
 *
 * * The base Element represents a state as detected by the [StateHierarchyProcessor]
 * * All lines of this Compartment are dashed lines by their associated [LineAttributes].
 *
 * If this is detected for a [PotentialCompartment]:
 * * The [CompartmentHotSpotDefinition] stored in the Element is inserted
 * * A new [OrthogonalRegion] Element is inserted
 * * A [RepresentsOrthogonalState] Relation is created between these two Elements
 * * A [ContainsRegion] relation is created between the State which is the [CompartmentElement.base] of the
 *   input [PotentialCompartment]
 *
 * This process is reversed whenever the conditions don't match any longer. This includes removing the
 * [CompartmentHotSpotDefinition] (which should then be recreated as a [PotentialCompartment] by the Compartment
 * processing.
 */
@ProcessorScoped
class StateCompartmentProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val idGenerator: IdGenerator,
        private val queries: RelationGraphQueries
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(
            StateElement::class.java,
            Represents::class.java,
            ShapedElement::class.java,
            PotentialCompartment::class.java,
            AttributesFor::class.java,
            LineAttributes::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val output = diffCollectionFactory.createMutableTimedDiffCollection()

        val applied = applicator.apply(diffs)

        // Maps the CompartmentElements for which the entire subtrees are already traversed to the result of the
        // traversal, i.e. if this Region sub-tree contains a OrthogonalRegion.
        val checkedCompartmentElements = mutableMapOf<ElementReference<CompartmentElement>, Boolean>()

        diffLoop@ for (diff in applied) {
            val affected = diff.affected

            if (affected is AttributesFor) {
                /*
                 * If AttributesFor relations have been added or removed, re-check the Compartments defined by the
                 * lines that were associated with it (because it may be that now the attributes are / are not
                 * suitable for Region lines. It cannot be decided based on the affected AttributeBag alone as there may be
                 * others that are also influential)
                 */
                checkAttributesFor(affected, output, checkedCompartmentElements)
            }

            if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                when (affected) {
                    is PotentialCompartment -> {
                        // If a PotentialCompartment has been added, check its properties and associated Elements
                        // Whether it can be converted to a CompartmentHotSpotDefinition
                        createOrthogonalRegionForDeepestCompartment(affected, output, checkedCompartmentElements)
                    }
                    is RepresentsStateElement -> {
                        val potentialCompartments = modelRepository.getRelationsFromElement(
                                affected.a.id,
                                PotentialCompartment::class.java,
                                true
                        )

                        for ((_, potential) in potentialCompartments) {
                            createOrthogonalRegionForDeepestCompartment(
                                    potential,
                                    output,
                                    checkedCompartmentElements
                            )
                        }
                    }
                    is OrthogonalRegion -> {
                        checkValidityOfRegion(affected, output, checkedCompartmentElements)
                    }
                    is LineAttributes -> {
                        /*
                         * if a LineAttributes Element has changed, check all lines for which that Element is assigned
                         * as an AttributeBag.
                         */
                        val attributeForRelations = modelRepository.getRelationsFromElement(
                                affected.id,
                                AttributesFor::class.java,
                                false
                        )

                        for ((_, attributeForRelation) in attributeForRelations) {
                            checkAttributesFor(attributeForRelation, output, checkedCompartmentElements)
                        }
                    }
                }
            } else if (diff is ElementRemoveDiff) {
                when (affected) {
                    is RepresentsOrthogonalState -> {
                        // If a RepresentsOrthogonalState relation is removed, remove the orthogonal state with it
                        output.mergeCollection(modelRepository.remove(affected.b.id))
                    }
                    is RepresentsStateElement -> {
                        if (modelRepository[affected.b] == null) {
                            // State has been removed with it; remove all regions
                            continue@diffLoop
                        }

                        // If a RepresentsStateElement relation is removed, all of its Regions die with the state
                        val containsRegionRelations = modelRepository.getRelationsToElement(
                                affected.b.id,
                                ContainsRegion::class.java,
                                false
                        )

                        for ((_, containsRegion) in containsRegionRelations) {
                            val region = modelRepository[containsRegion.b]

                            region?.let { removeOrthogonalRegion(it, output) }
                        }
                    }
                    is CompartmentContainsSubRegion -> {
                        val formerParent = modelRepository[affected.a]

                        if (formerParent != null) {
                            /*
                             * If the former parent hasn't also been deleted, use
                             */
                            createOrthogonalRegionForDeepestCompartment(
                                    formerParent,
                                    output,
                                    checkedCompartmentElements
                            )
                        }
                    }
                    // CompartmentHotSpotDefinition removal is automatically handled as the RepresentsOrthogonalState
                    // relation will be deleted along with it.
                }
            }
        }

        return output
    }

    /**
     * @return The parent state of the Region(s) found in this tree
     */
    private fun createOrthogonalRegionForDeepestCompartment(
            compartmentElement: CompartmentElement,
            output: MutableTimedDiffCollection,
            checkedElements: MutableMap<ElementReference<CompartmentElement>, Boolean>
    ) {
        var next = mutableMapOf<Pair<CompartmentHotSpotDefinition, State>?, List<CompartmentElement>>(null to listOf(compartmentElement))

        var regionFound = false

        var baseState: State? = null

        /*
         * Step one:
         * Go down in the hierarchy of Compartments dependent on the given compartmentElement.
         *
         * We have to analyze all dependent (potential) Compartments whether they are regions as only the ones deepest
         * in the hierarchy are to be inserted as OrthogonalRegions - else, we would also have a hierarchy of Orthogonal
         * Regions.
         */
        while (next.isNotEmpty()) {
            val current = next
            next = mutableMapOf()

            parentLoop@ for ((parent, dependentElements) in current) {
                // check all dependent elements whether they constitute an OrthogonalRegion
                var hasRegionDependency = false

                dependentLoop@ for (dependent in dependentElements) {
                    val ref = dependent.ref()
                    if (ref in checkedElements) {
                        // we have already checked this sub-tree and know the result
                        hasRegionDependency = hasRegionDependency || checkedElements[ref]!!

                        continue@dependentLoop
                    }

                    // check whether this dependent CompartmentHotSpotDefinition represents a state at all.
                    val newBaseState = findParentStateForCompartment(dependent)

                    if (newBaseState == null) {
                        // This (Potential) Compartment does not represent an OrthogonalRegion
                        // no need to bother further

                        checkedElements[ref] = false

                        continue@dependentLoop
                    }

                    if (baseState != null && newBaseState != baseState) {
                        throw IllegalStateException("A Compartment ${dependent.id} " +
                                "cannot have a different Parent State ${newBaseState.id} from the previous state" +
                                "in its hierarchy ${baseState.id}")
                    }

                    baseState = newBaseState

                    // If the dependent CompartmentElement is not a ComHSD yet, create it
                    val comHSD = (dependent as? CompartmentHotSpotDefinition)
                            ?: (dependent as? PotentialCompartment)?.let { tryInsertComHSD(it, output) }
                            ?: continue@dependentLoop

                    // insert the ContainsSubRegion relation for detecting when the representsRegion relation should be
                    // pushed further up
                    parent?.let { (parentHSD, _) ->
                        output.mergeCollection(
                                modelRepository.store(
                                        CompartmentContainsSubRegion(
                                                parentHSD.ref(),
                                                comHSD.ref()
                                        )
                                )
                        )
                    }

                    // Now enqueue this element with all of its dependencies for analysis in the next go
                    val toCheck = modelRepository.getRelationsFromElement(dependent.id).values.filterIsInstance<CompartmentElement>()

                    next[Pair(comHSD, baseState)] = toCheck

                    hasRegionDependency = true
                    regionFound = true
                }

                parent?.let { (comHSD, state) ->
                    checkedElements[comHSD.ref()] = true

                    if (hasRegionDependency) {
                        // If one of the dependent Compartments of the parent element represents
                        // a region, remove the region of the parent element, if present.
                        queries.getElementRelatedFrom(
                                comHSD.ref(),
                                RepresentsOrthogonalState::class.java,
                                false
                        )?.let {
                            output.mergeCollection(modelRepository.remove(it.id))
                        }
                    } else {
                        // if no regions could be found in a deeper compartment, then the current compartment must
                        // be the Region.
                        storeOrthogonalRegion(comHSD, state, output)
                    }
                }
            }
        }

        /*
         * Step two:
         * Go up in the dependency hierarchy, i.e. visit all Compartments the compartmentElement depends on,
         * remove all Regions for parent (split) Compartments if a region was found in a deeper Compartment.
         */
        if (regionFound) {
            var current = (compartmentElement as? PotentialCompartment)?.hsd
                    ?: (compartmentElement as? CompartmentHotSpotDefinition)

            while (current != null) {
                val nextParent = current.splitCompartment ?: break

                val nextParentElement = modelRepository[nextParent]!!

                modelRepository.getRelationsFromElement(nextParent.id, RepresentsOrthogonalState::class.java, false).forEach { (_, rel) ->
                    output.mergeCollection(modelRepository.remove(rel.b.id))
                }

                output.mergeCollection(modelRepository.store(CompartmentContainsSubRegion(nextParent, current.ref())))

                current = nextParentElement
            }
        }
    }

    /**
     * Inserts and returns the [CompartmentHotSpotDefinition] contained in [PotentialCompartment.hsd] if the dependency of the
     * [element] is already satisfied, or returns null if not.
     */
    private fun tryInsertComHSD(
            element: PotentialCompartment,
            output: MutableTimedDiffCollection
    ): CompartmentHotSpotDefinition? {
        if (element.splitCompartment != null && this.modelRepository[element.splitCompartment] == null) {
            return null
        }

        val hsd = element.hsd

        output.mergeCollection(modelRepository.store(hsd))

        return hsd
    }

    /**
     * Checks whether the given [region] still has a valid HotSpotDefinition associated to it and its
     * [ContainsRegion] relations are set accordingly.
     *
     * If not, the [OrthogonalRegion] objects are removed, or the [ContainsRegion] relations set to their correct values.
     */
    private fun checkValidityOfRegion(
            region: OrthogonalRegion,
            output: MutableTimedDiffCollection,
            checkedElements: MutableMap<ElementReference<CompartmentElement>, Boolean>,
            givenRepresentingCompartment: CompartmentHotSpotDefinition? = null
    ) {
        val representingCompartment = givenRepresentingCompartment ?: queries.getElementRelatedTo(
                region.ref(),
                RepresentsOrthogonalState::class.java,
                false
        )

        if (representingCompartment == null) {
            output.mergeCollection(modelRepository.remove(region.id))
            return
        }

        createOrthogonalRegionForDeepestCompartment(
                representingCompartment,
                output,
                checkedElements
        )
    }

    /**
     * Checks whether the elements dependent on the [AttributesFor.b] GraphicObject now represent a valid orthogonal
     * Region with the changed [AttributesFor.a] Attributes.
     */
    private fun checkAttributesFor(
            attributesFor: AttributesFor,
            output: MutableTimedDiffCollection,
            checkedElements: MutableMap<ElementReference<CompartmentElement>, Boolean>
    ) {
        val aElement = modelRepository[attributesFor.a]
        val bElement = modelRepository[attributesFor.b]

        // AttributesFor relations are currently only relevant for Line objects.
        if (bElement !is GraphicObject<*> || bElement.shape !is Line) {
            return
        }

        // currently, only LineAttribute bags are relevant for Region detection.
        // However, if the aElement is null (= does not exist anymore), also check the compartments.
        if (aElement != null && aElement !is LineAttributes) {
            return
        }

        // first, get all CompartmentElements that have the attributed line as one of their line dependencies...
        val potentialCompartments = modelRepository.getRelationsToElement(
                bElement.id,
                PotentialCompartment::class.java
        )

        // ... and check whether they now (with the changed attributes) maybe qualify as an orthogonal state
        for ((_, potentialCompartment) in potentialCompartments) {
            createOrthogonalRegionForDeepestCompartment(potentialCompartment, output, checkedElements)
        }

        val comHSDs = modelRepository.getRelationsToElement(
                bElement.id,
                CompartmentHotSpotDefinition::class.java
        )

        for ((_, comHSD) in comHSDs) {
            createOrthogonalRegionForDeepestCompartment(comHSD, output, checkedElements)
        }

    }

    /**
     * Creates and saves an OrthogonalRegion Element with the given parameters.
     *
     * @param comHSD      The CompartmentHotSpotDefinition that should be associated with the new Region.
     * @param parentState The state that should be stored as the superordinate state for the new Region.
     */
    private fun storeOrthogonalRegion(
            comHSD: CompartmentHotSpotDefinition,
            parentState: State,
            output: MutableTimedDiffCollection
    ) {
        val currentRepresents = modelRepository.getRelationsFromElement(
                comHSD.id,
                RepresentsOrthogonalState::class.java,
                false
        )


        when {
            currentRepresents.isEmpty() -> {
                // No orthogonal regions currently
                val orthogonalRegion = OrthogonalRegion(idGenerator.generate(OrthogonalRegion::class.java))

                val represents = RepresentsOrthogonalState(comHSD.ref(), orthogonalRegion.ref())

                val containsRegion = ContainsRegion(parentState.ref(), orthogonalRegion.ref())

                output.mergeCollection(modelRepository.store(orthogonalRegion))
                output.mergeCollection(modelRepository.store(represents))
                output.mergeCollection(modelRepository.store(containsRegion))
            }
            currentRepresents.size == 1 -> {
                val representsRelation = currentRepresents.values.first()

                val currentContainsRegions = modelRepository.getRelationsToElement(
                        representsRelation.b.id,
                        ContainsRegion::class.java,
                        false
                )

                if (currentContainsRegions.size > 1) {
                    throw IllegalStateException("A OrthogonalRegion Element ${representsRelation.b.id} " +
                            "cannot have more than one ContainsRegion relation pointing to it.")
                }

                val currentContainsRegion = currentContainsRegions.values.firstOrNull()
                if (currentContainsRegion != null) {
                    // If the OrthogonalRegion represented by ComHSD is already contained in a state, check that relation.
                    if (currentContainsRegion.a == parentState.ref()) {
                        // Region is already correctly associated with the given parent state. Nothing to do here
                        return
                    }

                    // else, remove this ContainsRegion Relation.
                    output.mergeCollection(modelRepository.remove(currentContainsRegion.id))
                }

                // If no correct ContainsRegion Relation has been found, add a new one.
                val containsRegion = ContainsRegion(parentState.ref(), representsRelation.b)

                output.mergeCollection(modelRepository.store(containsRegion))
            }
            else -> throw IllegalStateException(
                    "ComHSD ${comHSD.id} has multiple (${currentRepresents.size}) " +
                            "RepresentsOrthogonalState Relations, expected 1: " +
                            currentRepresents.values.joinToString(",", "{", "}", -1, "", RepresentsOrthogonalState::id)
            )
        }

    }

    /**
     * Removes the given Region and all of its dependencies.
     * This also means that the corresponding [CompartmentHotSpotDefinition] is removed
     */
    private fun removeOrthogonalRegion(
            region: OrthogonalRegion,
            output: MutableTimedDiffCollection
    ) {
        val representingHotSpotDefinition = queries.getElementRelatedTo(
                region.ref(),
                RepresentsOrthogonalState::class.java,
                false
        )

        output.mergeCollection(modelRepository.remove(region.id))
        representingHotSpotDefinition?.let { output.mergeCollection(modelRepository.remove(it.id)) }
    }

    /**
     * Returns the parent state if the given [compartmentElement] correctly represents a orthogonal state Region
     * in the UML state chart notation.
     *
     * @param compartmentElement Compartment data object to check whether it is the concrete syntax representation of a
     *                           [OrthogonalRegion]
     *
     * @return The parent state that applies to this [compartmentElement], or null if the [compartmentElement] cannot
     *         represent an [OrthogonalRegion].
     */
    private fun findParentStateForCompartment(compartmentElement: CompartmentElement): State? {
        val baseElement = compartmentElement.base
        val lineElements = compartmentElement.lineSet

        val baseElementState = queries.getElementRelatedFrom(
                baseElement,
                RepresentsStateElement::class.java,
                true
        ) as? State ?: return null

        // The compartmentElement is only a valid region if all lines it depends on are dashed
        val allLinesDashed = lineElements.all { lineElement ->
            var attributeBagsForLine = queries.getElementsRelatedTo(
                    lineElement,
                    AttributesFor::class.java,
                    true
            ).filterIsInstance<LineAttributes>()

            if (attributeBagsForLine.isEmpty()) {
                attributeBagsForLine = listOf(LineAttributes("tmp"))
            }

            return@all attributeBagsForLine.all { it.getLineStyle() == LineStyle.Option.DASHED }
        }

        return if (allLinesDashed) baseElementState else null
    }

    /**
     * This relation is for internal use by the StateCompartmentProcessor.
     *
     * That is owed to the specifics of the Compartment system - only the visible Regions should be seen as the
     * [OrthogonalRegion]s of a state, whereas the [CompartmentHotSpotDefinition]s may be hierarchical if one Compartment's
     * shape depends on the others.
     *
     * Only the Compartments that don't have other Region-representing Compartments as their dependencies
     * may thus be inserted as [OrthogonalRegion]s.
     *
     * The CompartmentContainsSubRegion Relation represents that the [a] Compartment **would** represent a Region, but as
     * the [b] Compartment depends on it, the [OrthogonalRegion] is not inserted.
     */
    private class CompartmentContainsSubRegion(
            a: ElementReference<CompartmentHotSpotDefinition>,
            b: ElementReference<CompartmentHotSpotDefinition>
    ) : OneToOneRelation<CompartmentHotSpotDefinition, CompartmentHotSpotDefinition>(a, b) {
        override fun copy() = CompartmentContainsSubRegion(a, b)

        override var probability: ProbabilityInfo? = ProbabilityInfo.Explicit
    }
}
