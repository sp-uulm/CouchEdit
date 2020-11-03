package de.uulm.se.couchedit.processing.spatial.controller

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition
import de.uulm.se.couchedit.model.spatial.relations.SpatialRelation
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementRemoveDiff
import de.uulm.se.couchedit.processing.common.model.ModelDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.result.ElementQueryResult
import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.util.extensions.ref

@ProcessorScoped
class SpatialAbstractor @Inject constructor(
        private val repository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val diffApplicator: Applicator,
        private val spatialRelationChecker: SpatialRelationChecker
) : Processor {
    override fun consumes(): List<Class<out Element>> {
        return listOf(ShapedElement::class.java)
    }

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val appliedDiffs = this.diffApplicator.apply(diffs)

        // collect all ShapedElements whose shapes depend on modified elements from appliedDiffs. We will check their
        // relations as well (as the shapes may have changed)
        val relatedShapedElements = mutableMapOf<String, ShapedElement<*>>()

        val processedOtherElements = mutableSetOf<ElementReference<*>>()

        for (diff in appliedDiffs.diffs.values) {
            relatedShapedElements.putAll(
                    this.processDiff(diff, ret, appliedDiffs.getVersionForElement(diff.affected.id), processedOtherElements)
            )
        }

        // Prevent endless loops by checking every related element only once
        val processedShapedElements = mutableSetOf<ShapedElement<*>>()

        while (relatedShapedElements.isNotEmpty()) {
            val hotSpotDef = relatedShapedElements.values.first()

            processedShapedElements.add(hotSpotDef)

            relatedShapedElements.putAll(
                    this.calculateSpatialRelations(hotSpotDef, ret, processedOtherElements).filterNot { it.value in processedShapedElements }
            )

            relatedShapedElements.remove(hotSpotDef.id)
        }

        for (refreshId in appliedDiffs.refreshs) {
            if(refreshId !in diffs.diffs) {
                ret.mergeCollection(repository.refresh(refreshId))
            }
        }

        return ret
    }

    private fun processDiff(
            toApply: ModelDiff,
            output: MutableTimedDiffCollection,
            timestamp: VectorTimestamp,
            processedOtherElements: MutableSet<ElementReference<*>>
    ): ElementQueryResult<ShapedElement<*>> {
        if (toApply is ElementRemoveDiff) {
            output.putDiff(toApply, timestamp)

            return ElementQueryResult()
        }

        return (toApply.affected as? ShapedElement<*>)?.let {
            this.calculateSpatialRelations(it, output, processedOtherElements)
        } ?: ElementQueryResult()
    }

    private fun calculateSpatialRelations(
            g: ShapedElement<*>,
            output: MutableTimedDiffCollection,
            processedOtherElements: MutableSet<ElementReference<*>>
    ): ElementQueryResult<ShapedElement<*>> {
        val allGraphicObjects = this.repository.getAllIncludingSubTypes(ShapedElement::class.java)

        val existingRelationsToRemove = this.repository.getRelationsAdjacentToElement(
                g.id,
                SpatialRelation::class.java,
                true
        ).toMutableMap()

        val gRef = g.ref()

        /*
         * this is inefficient. Would be nice to have:
         * - Check for existing relations first, optimizing checking if they still hold
         * - Transitivity
         * - ...?
         */
        for ((id, value) in allGraphicObjects) {
            if (value == g) {
                continue
            }

            val otherRef = value.ref()

            if (otherRef in processedOtherElements) {
                val iterator = existingRelationsToRemove.iterator()

                // remove all relations towards this Element from the relations to remove.
                while (iterator.hasNext()) {
                    val (_, rel) = iterator.next()

                    if (rel.a == otherRef || rel.b == otherRef) {
                        iterator.remove()
                    }
                }

                continue
            }

            val currentSpatialRelations = this.spatialRelationChecker.getSpatialRelations(gRef, otherRef)

            for (relation in currentSpatialRelations) {
                existingRelationsToRemove.remove(relation.id)

                output.mergeCollection(this.repository.store(relation))
            }
        }

        for ((id, _) in existingRelationsToRemove) {
            output.mergeCollection(this.repository.remove(id))
        }

        processedOtherElements.add(gRef)

        // Now, return all adjacent HotSpotDefinitions so we know that we have to re-check the spatial relations of
        // these, too
        // TODO more generic?
        return this.repository.getRelationsAdjacentToElement(g.id, HotSpotDefinition::class.java, true)
    }
}
