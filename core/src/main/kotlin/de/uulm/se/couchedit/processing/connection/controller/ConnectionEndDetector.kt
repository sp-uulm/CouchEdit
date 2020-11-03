package de.uulm.se.couchedit.processing.connection.controller

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import de.uulm.se.couchedit.model.base.suggestions.PotentialRelation
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.connection.relations.DirectedConnection
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.model.graphic.shapes.Shape
import de.uulm.se.couchedit.model.spatial.relations.Disjoint
import de.uulm.se.couchedit.model.spatial.relations.Intersect
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ServiceCaller
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.spatial.services.geometric.NearestPointCalculator
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import de.uulm.se.couchedit.util.extensions.ref
import de.uulm.se.couchedit.util.math.Decay

@ProcessorScoped
class ConnectionEndDetector @Inject constructor(
        private val modelRepository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val applicator: Applicator,
        private val serviceCaller: ServiceCaller,
        private val shapeExtractor: ShapeExtractor,
        private val nearestPointCalculator: NearestPointCalculator
) : Processor {
    override fun consumes(): List<Class<out Element>> {
        return listOf(
                Disjoint::class.java,
                Intersect::class.java,
                GraphicObject::class.java,
                DirectedConnection::class.java,
                ConnectionEnd::class.java
        )
    }

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val appliedDiffs = applicator.apply(diffs)

        for (diff in appliedDiffs) {
            if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                when (val el = diff.affected) {
                    is Disjoint -> {
                        checkPossibleConnectionOf(el.a, el.b, ret)
                    }
                    is ShapedElement<*> -> {
                        val disjointRels = this.modelRepository.getRelationsAdjacentToElement(el.id, Disjoint::class.java, true).values
                        val intersectRels = this.modelRepository.getRelationsAdjacentToElement(el.id, Intersect::class.java, true).values

                        disjointRels.union(intersectRels).forEach {
                            this.checkPossibleConnectionOf(it.a, it.b, ret)
                        }
                    }
                }
            }

            (diff.affected as? ConnectionEnd<*, *>)?.let {
                // ensure that if a ConnectionEnd gets added, we remove the suggestion / if it is removed, check it again
                checkPossibleConnectionOf(it.a, it.b, ret)
            }
        }

        // republish all removals of potential connection ends that were removed e.g. as a result of the line being removed.
        ret.mergeCollection(appliedDiffs.filter { (it.affected as? PotentialRelation<*, *, *>)?.type == ConnectionEnd::class.java })

        return ret
    }

    private fun checkPossibleConnectionOf(aRef: ElementReference<*>, bRef: ElementReference<*>, output: MutableTimedDiffCollection) {
        if (aRef.referencesType(ShapedElement::class.java) && bRef.referencesType(ShapedElement::class.java) &&
                (aRef.referencesType(GraphicObject::class.java) || bRef.referencesType(GraphicObject::class.java))) {
            val aElement = this.modelRepository[aRef.asType<ShapedElement<Shape>>()] ?: return
            val bElement = this.modelRepository[bRef.asType<ShapedElement<Shape>>()] ?: return

            val (lineElement, otherElement) =
                    @Suppress("UNCHECKED_CAST") // Per type definitions type parameter of GraphicObject is = type of shape
                    when {
                        (aElement as? GraphicObject<*>)?.shape is Line -> Pair(aElement as GraphicObject<Line>, bElement)
                        (bElement as? GraphicObject<*>)?.shape is Line -> Pair(bElement as GraphicObject<Line>, aElement)
                        else -> return
                    }

            val lineShape = lineElement.shape
            val otherShape = serviceCaller.call(otherElement.ref(), shapeExtractor::extractShape) ?: return

            val existingRelations = modelRepository.getRelationsBetweenElements(
                    lineElement.id,
                    otherElement.id,
                    ConnectionEnd::class.java
            ).values

            checkRelation(lineElement, otherElement, lineShape, otherShape, false, existingRelations, output)
            checkRelation(lineElement, otherElement, lineShape, otherShape, true, existingRelations, output)
        }
    }

    private fun checkRelation(
            lineElement: GraphicObject<Line>,
            otherElement: ShapedElement<*>,
            lineShape: Line,
            otherShape: Shape,
            checkEnd: Boolean,
            existingRelations: Collection<ConnectionEnd<*, *>>,
            output: MutableTimedDiffCollection
    ) {
        if (!lineElement.attachedTo(otherElement.id, checkEnd)) {
            val point = if (checkEnd) lineShape.end else lineShape.start
            val pointId = lineElement.id + "_" + (if (checkEnd) "end" else "start")

            val prob =
                    if (nearestPointCalculator.isDistanceUnder(
                                    point,
                                    otherShape,
                                    DISTANCE_THRESHOLD,
                                    pointId,
                                    otherElement.id
                            )) {
                        // calculate the distance to the outline so that lines to sub-elements are not always also
                        // recognized as connections to the superordinate Element.
                        val distance = nearestPointCalculator.calculateBorderDistanceBetween(
                                point,
                                otherShape,
                                pointId,
                                otherElement.id
                        )

                        if (distance < DISTANCE_THRESHOLD) decayFunction.calculate(distance)
                        else -1.0

                    } else -1.0

            updateRelation(lineElement, otherElement, prob, checkEnd, existingRelations, output)
        }
    }

    private fun updateRelation(
            line: GraphicObject<Line>,
            other: ShapedElement<*>,
            probability: Double,
            isEnd: Boolean,
            existingRelations: Collection<ConnectionEnd<*, *>>,
            output: MutableTimedDiffCollection
    ) {
        val existing = existingRelations.find { it.isEndConnection == isEnd }

        if (probability >= 0) {
            val probInfo = ProbabilityInfo.Generated(probability)

            if (existing == null) {
                this.modelRepository.store(ConnectionEnd(line.ref(), other.ref(), isEnd, probInfo))
            } else {
                existing.probability = probInfo
                this.modelRepository.store(existing)
            }.let(output::mergeCollection)
        } else {
            if (existing != null) {
                modelRepository.remove(existing.id).let(output::mergeCollection)
            }
        }
    }

    private fun GraphicObject<*>.attachedTo(otherId: String, isEnd: Boolean): Boolean {
        return modelRepository.getRelationsBetweenElements(this.id, otherId, ConnectionEnd::class.java).values.find { element ->
            (element as? ConnectionEnd)?.let { it.probability == ProbabilityInfo.Explicit && it.isEndConnection == isEnd } == true
        } != null
    }

    companion object {
        // TODO configurable distance threshold?
        const val DISTANCE_THRESHOLD = 20.0

        private val decayFunction = Decay(
                exponent = -0.5,
                preFactor = 0.2,
                postFactor = 1.2,
                yDisplacement = -0.2,
                maxValue = 0.9
        )
    }
}
