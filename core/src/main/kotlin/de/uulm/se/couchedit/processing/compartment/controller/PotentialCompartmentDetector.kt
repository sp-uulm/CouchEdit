package de.uulm.se.couchedit.processing.compartment.controller

import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.compartment.CompartmentElement
import de.uulm.se.couchedit.model.compartment.CompartmentHotSpotDefinition
import de.uulm.se.couchedit.model.compartment.PotentialCompartment
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.spatial.relations.Include
import de.uulm.se.couchedit.model.spatial.relations.Intersect
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.compartment.services.CompartmentGeometryGenerator
import de.uulm.se.couchedit.processing.compartment.services.CompartmentServiceCaller
import de.uulm.se.couchedit.processing.compartment.services.SplitResultInterpreter
import de.uulm.se.couchedit.processing.graphic.util.ZOrderComparator
import de.uulm.se.couchedit.util.extensions.ref
import java.util.*
import javax.inject.Inject

/**
 * Processor that, in its current form, detects any potential compartmentalization of a [GraphicObject] by
 * one or more other [GraphicObject]s of which the shape is a line, and adds a [CompartmentHotSpotDefinition]
 * for every one of these compartments.
 */
@ProcessorScoped
class PotentialCompartmentDetector @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val serviceCaller: ServiceCaller,
        private val compartmentServiceCaller: CompartmentServiceCaller,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val compartmentGeometryGenerator: CompartmentGeometryGenerator,
        private val splitResultInterpreter: SplitResultInterpreter
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(
            ShapedElement::class.java,
            Include::class.java,
            Intersect::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val output = diffCollectionFactory.createMutableTimedDiffCollection()

        val changes = applicator.apply(diffs)

        val checkedElements = mutableSetOf<ElementReference<*>>()
        for (change in changes) {
            val affected = change.affected
            if (change is ElementAddDiff || change is ElementModifyDiff) {
                if (affected is GraphicObject<*>) {
                    val potentialBaseElements = mutableSetOf<ElementReference<GraphicObject<*>>>()

                    if (Line::class.java.isAssignableFrom(affected.shapeClass)) {
                        // when a line has changed, check all elements it intersects or that include it
                        val compartmentingRelations = mutableSetOf<SpatialRelation>()

                        compartmentingRelations.addAll(modelRepository.getRelationsToElement(affected.id, Include::class.java, true).values)
                        compartmentingRelations.addAll(modelRepository.getRelationsAdjacentToElement(affected.id, Intersect::class.java, true).values)

                        for (relation in compartmentingRelations) {
                            val potentialBaseElement = if (relation.a.id == affected.id) relation.b else relation.a

                            // only use include / intersect with GraphicObjects, not other HotSpotProviders
                            if (potentialBaseElement.referencesType(GraphicObject::class.java)) {
                                potentialBaseElements.add(potentialBaseElement.asType())
                            }
                        }
                    } else {
                        // if a non-line ShapedElement has changed, check that element's relations
                        potentialBaseElements.add(affected.ref())
                    }

                    for (element in potentialBaseElements) {
                        this.checkAndGeneratePotentialCompartments(
                                element,
                                output,
                                checkedElements
                        )
                    }
                }

                if (affected is CompartmentHotSpotDefinition) {
                    onCompartmentInsert(affected, output)
                }
            } else if (change is ElementRemoveDiff && affected is CompartmentElement) {
                /*
                 * if a CompartmentElement has been removed, check its base element which new Compartments can be
                 * generated.
                 * Maybe we must insert PotentialCompartments again as a replacement.
                 */
                this.checkAndGeneratePotentialCompartments(
                        affected.base,
                        output,
                        checkedElements
                )
            }

            // Check potential compartments also if Intersection or Include has been detected after element
            // insertion.
            if (affected is Include || affected is Intersect) {
                affected as SpatialRelation

                if (affected.b.referencesType(GraphicObject::class.java)) {
                    val bElement = this.modelRepository[affected.b]

                    if (affected.a.referencesType(GraphicObject::class.java)
                            && (bElement as? GraphicObject)?.shapeClass?.let {
                                Line::class.java.isAssignableFrom(it)
                            } == true
                    ) {
                        checkAndGeneratePotentialCompartments(
                                affected.a.asType(),
                                output,
                                checkedElements
                        )

                        continue
                    }
                }

                if (affected is Intersect) {
                    if (affected.a.referencesType(GraphicObject::class.java)) {
                        val aElement = this.modelRepository[affected.a]

                        if (affected.b.referencesType(GraphicObject::class.java)
                                && (aElement as? GraphicObject)?.shapeClass?.let {
                                    Line::class.java.isAssignableFrom(it)
                                } == true
                        ) {
                            checkAndGeneratePotentialCompartments(
                                    affected.b.asType(),
                                    output,
                                    checkedElements
                            )
                        }
                    }
                }
            }
        }

        return output
    }

    /**
     * Executes appropriate application data operations into [output] when [affected] has been added.
     *
     * This includes removing the prior [PotentialCompartment] for [affected] and modifying all of its formerly dependent
     * Elements to now point to [affected].
     */
    private fun onCompartmentInsert(
            affected: CompartmentHotSpotDefinition,
            output: MutableTimedDiffCollection
    ) {
        // find former "potential" representation for the newly inserted HotSpotDefinition
        val potentialCompartment = modelRepository.getRelationsFromElement(
                affected.base.id,
                PotentialCompartment::class.java,
                false
        ).values.firstOrNull {
            it.lineSet == affected.lineSet
                    && it.hsd.splitCompartment == affected.splitCompartment
                    && it.index == affected.index
        }

        // Check all CompartmentElements dependent on the (potential) compartment
        val dependencies = modelRepository.getRelationsFromElement(potentialCompartment?.id
                ?: affected.id).values.filterIsInstance(CompartmentElement::class.java).toMutableSet()

        // remove the potential compartment (and all related compartment elements)
        potentialCompartment?.let { output.mergeCollection(modelRepository.remove(it.id)) }

        for (dependency in dependencies) {
            if (dependency is PotentialCompartment) {
                check(dependency.hsd.splitCompartment == affected.ref()) {
                    "The potential HotSpotDefinition must have ${affected.ref()} " +
                            "as its splitCompartment as it previously had its \"potential\" representation as its" +
                            " potentialSplitCompartment. Actual value: ${dependency.hsd.splitCompartment}"
                }

                /*
                 * Insert a new PotentialCompartment for the dependency but depending on the "real"
                 * CompartmentHotSpotDefinition instead just the PotentialCompartment
                 */
                output.mergeCollection(modelRepository.store(PotentialCompartment(dependency.hsd.copy(), null)))
            }
        }
    }

    /**
     * Iteratively scans the [baseElement] for GraphicObjects with [Line] shapes and tries to split the shape of the
     * [baseElement], then tries to split the already found compartments, ... until no new compartments are found
     * anymore.
     *
     * The resulting changes in the data model are written to the [output] DiffCollection.
     */
    private fun checkAndGeneratePotentialCompartments(
            baseElement: ElementReference<GraphicObject<*>>,
            output: MutableTimedDiffCollection,
            alreadyCheckedElements: MutableSet<ElementReference<*>>
    ) {
        if (baseElement in alreadyCheckedElements) {
            return
        }

        alreadyCheckedElements.add(baseElement)

        val lines = this.getPotentialSplittingGraphicObjects(modelRepository[baseElement] ?: return)

        /*
         * Get current HotSpotDefinitions. During processing, we will remove all from the list that are still valid.
         */
        val compartmentElementsToRemove = this.modelRepository.getRelationsFromElement(
                baseElement.id,
                CompartmentHotSpotDefinition::class.java,
                true
        ).values.toMutableSet<CompartmentElement>()

        compartmentElementsToRemove.addAll(this.modelRepository.getRelationsFromElement(
                baseElement.id,
                PotentialCompartment::class.java,
                true
        ).values)

        /*
         * It is not sufficient to just calculate the Compartments once, as Compartments can be again dependent on other
         * Compartment's areas.
         * As we want to detect all these Compartments, we need to iteratively check if there are any more known
         * possibilities before we can return a complete set of HotSpotDefinitions.
         */

        // Queue of elements to try and find sub-compartments of.
        val elementQueue = LinkedList<CompartmentElement>()

        do {
            // generate the set of HotSpotDefinitions based on the currently considered Element.

            // The subElement variable contains the HotSpotDefinition that is currently viewed.
            val subElement = if (elementQueue.isNotEmpty()) elementQueue.removeFirst() else null

            val results = getCompartments(baseElement, subElement, lines, compartmentElementsToRemove)

            // we need to check if we get further sub-divisions of compartments that we already have
            elementQueue.addAll(results)

            for (result in results) {
                output.mergeCollection(this.modelRepository.store(result))
            }
        } while (!elementQueue.isEmpty())

        compartmentElementsToRemove.forEach {
            output.mergeCollection(modelRepository.remove(it.id))
        }
    }

    /**
     * @param baseElement The base GraphicObject Element that will be split into Compartments
     * @param subElement If this is set, the area of the Compartment represented by that HotSpotDefinition will be split
     *                   instead of the [baseElement]'s area
     * @param lines Set of GraphicObjects that have a [Line] as their Shape. These are the Lines that the Base area (or
     *              the subElement area) will be split.
     * @param toRemove Set of pre-existing [CompartmentHotSpotDefinition]s. The function will remove those that are
     *                 still valid for the given combination of [baseElement], [subElement] and [lines].
     *
     * @return Set of new [CompartmentHotSpotDefinition]s that result from the given combination of
     *         [baseElement], [subElement] and [lines].
     *
     *         The real set of [CompartmentHotSpotDefinition]s is the combination of the Elements removed from [toRemove]
     *         and the return set.
     */
    private fun getCompartments(
            baseElement: ElementReference<GraphicObject<*>>,
            subElement: CompartmentElement?,
            lines: Set<GraphicObject<*>>,
            toRemove: MutableSet<CompartmentElement>
    ): Set<CompartmentElement> {
        val ret = mutableSetOf<CompartmentElement>()

        // Try to split into compartments
        val compartmentSplitter = { ref: ElementReference<ShapedElement<*>>, related: Map<ElementReference<*>, Element> ->
            compartmentGeometryGenerator.generateGeometries(ref, related, lines)
        }

        val result = if (subElement == null) {
            serviceCaller.call(baseElement, compartmentSplitter)!!
        } else {
            /*
             * If we want to get the shapes from a CompartmentElement, then we need to use the compartmentServiceCaller
             * which is able to resolve PotentialCompartment and its relations to "real" shapedElements.
             */
            compartmentServiceCaller.call(subElement.ref(), compartmentSplitter)!!
        }

        val dependencies = result.usedLines.toSet()

        val indexes = result.indexes.toMutableMap()
        val indexesReverse = result.indexes.inverse().toMutableMap()

        val i = toRemove.iterator()
        /*
         * first, check if we have an applicable definition (in the existing HotSpotDefinitions = toRemove Set
         */
        existingDefinitionLoop@ while (i.hasNext()) {
            val compartmentElement = i.next()

            if (compartmentElement.lineSet != dependencies) {
                continue@existingDefinitionLoop
            }

            if (compartmentElement.splitCompartment != subElement?.ref()) {
                continue@existingDefinitionLoop
            }

            val index = compartmentElement.index
            val matchingGeometry = splitResultInterpreter.getGeometryByIndex(result, compartmentElement.index)

            if (matchingGeometry != null) {
                if (index.interiorPoint == null) {
                    indexes.remove(index)
                } else {
                    val indexToRemove = indexesReverse.remove(matchingGeometry)
                    indexes.remove(indexToRemove)
                }

                // definition is still valid. continue with next cell.
                ret.add(compartmentElement)
                i.remove()

                continue@existingDefinitionLoop
            }
        }

        // Each index of the result gets its own HotSpotDefinition.
        cellLoop@ for (index in indexes.keys) {
            /*
             * there are two cases here:
             *
             * 1. The subElement, i.e. the Compartment the currently detected Compartments depend on, is a "real"
             *    CompartmentHotSpotDefinition. PotentialSub then is null and we can use the subElement directly as the
             *    dependency of the new PotentialCompartment.
             * 2. The subElement is a PotentialCompartment itself. This means that the "actual" ShapedElement to be checked
             *    is the HotSpotDefinition contained inside subShapedElement
             */
            val potentialSub = (subElement as? PotentialCompartment)

            val subShapedElement = potentialSub?.hsd ?: (subElement as? CompartmentHotSpotDefinition)

            // if no existing definition has been found, create a new one.
            val hotSpotDefinition = CompartmentHotSpotDefinition(baseElement, subShapedElement?.ref(), dependencies, index)

            val potential = PotentialCompartment(hotSpotDefinition, potentialSub?.ref())

            ret.add(potential)
        }

        return ret
    }

    /**
     * From the [ModelRepository]'s SpatialRelations ([Include] and [Intersect]), finds the [GraphicObject]s with
     * a [Line] as their shape which are potentially creating a compartment within the given [baseElement].
     *
     * This includes all GraphicObjects that have a [Line] as their shape and either intersect the given [baseElement]
     * and are included in it.
     * Furthermore, the lines must have at least equal Z-Coordinate as the [baseElement].
     */
    private fun getPotentialSplittingGraphicObjects(baseElement: GraphicObject<*>): Set<GraphicObject<*>> {
        // Get all intersecting and included lines (todo: distinguish line types for prioritization)
        val allIncludeRelations = modelRepository.getRelationsFromElement(baseElement.id, Include::class.java, true)

        val lines = allIncludeRelations.values.mapNotNull { modelRepository[it.b] as? GraphicObject }.filter {
            Line::class.java.isAssignableFrom(it.shapeClass)
        }.toMutableSet()

        val allIntersectRelations = modelRepository.getRelationsAdjacentToElement(baseElement.id, Intersect::class.java, true)
        val intersectingLines = allIntersectRelations.values.flatMap {
            listOf(
                    modelRepository[it.a] as? GraphicObject,
                    modelRepository[it.b] as? GraphicObject
            )
        }.filter {
            it != null && Line::class.java.isAssignableFrom(it.shapeClass)
        }.filterNotNull()

        lines.addAll(intersectingLines)

        // restrict compartmentalization to GraphicObjects in front of the given baseElement (or on the same level)
        return lines.filter { ZOrderComparator.compare(it, baseElement) >= 0 }.toSet()
    }
}
