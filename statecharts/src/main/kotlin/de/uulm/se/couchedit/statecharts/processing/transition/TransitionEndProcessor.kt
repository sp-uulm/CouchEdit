package de.uulm.se.couchedit.statecharts.processing.transition

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.attribute.elements.AttributesFor
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.connection.relations.ConnectionEnd
import de.uulm.se.couchedit.model.graphic.attributes.LineAttributes
import de.uulm.se.couchedit.model.graphic.attributes.types.LineEndPointStyle
import de.uulm.se.couchedit.model.graphic.attributes.types.LineStyle
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.graphic.shapes.Line
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.ElementAddDiff
import de.uulm.se.couchedit.processing.common.model.ElementModifyDiff
import de.uulm.se.couchedit.processing.common.model.diffcollection.MutableTimedDiffCollection
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.queries.RelationGraphQueries
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.statecharts.model.couch.elements.StateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.RepresentsStateElement
import de.uulm.se.couchedit.statecharts.model.couch.relations.representation.transition.TransitionEndPoint
import de.uulm.se.couchedit.util.extensions.ref

@ProcessorScoped
class TransitionEndProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val applicator: Applicator,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val queries: RelationGraphQueries
) : Processor {
    override fun consumes(): List<Class<out Element>> = listOf(
            StateElement::class.java,
            RepresentsStateElement::class.java,
            GraphicObject::class.java,
            AttributesFor::class.java,
            LineAttributes::class.java,
            ConnectionEnd::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val appliedDiffs = applicator.apply(diffs)

        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        // store the combinations (ConnectionEnd, State) for which we have already checked whether they constitute a
        // TransitionEnd in this run to save a little bit of processing time.
        val checkedElementCombinations = mutableSetOf<Pair<String, String>>()

        diffLoop@ for (diff in appliedDiffs) {
            val affected = diff.affected

            if (diff is ElementAddDiff || diff is ElementModifyDiff) {
                // removal of ConnectionEnd and RepresentsStateElement is automatically handled by the ModelRepository
                when (affected) {
                    is ConnectionEnd<*, *> -> {
                        // If a ConnectionEnd has been inserted or modified (probability!), check the two related objects.
                        ret.mergeCollection(checkConnectionEnd(affected, null, null, checkedElementCombinations))
                    }
                    is RepresentsStateElement -> {
                        // if a new state element has been inserted, we need to check all of its representing GraphicObject's
                        // ConnectionEnds.
                        modelRepository[affected.b]?.let {
                            ret.mergeCollection(checkTransitionEndPointsOfState(it, affected.a, checkedElementCombinations))
                        }
                    }
                    is LineAttributes -> {
                        // if Attributes have been removed, we can't fetch their attributed Elements anymore.
                        // This case is handled by the AttributesFor branch of the below When.

                        val attributedElements = queries.getElementsRelatedFrom(
                                affected.ref(),
                                AttributesFor::class.java,
                                false
                        )

                        // If LineAttributes have been changed, check all of the ConnectionEnds for this Line
                        for (element in attributedElements) {
                            if (element !is GraphicObject<*> || element.shape !is Line) {
                                continue
                            }

                            ret.mergeCollection(checkTransitionEndPointsForLine(
                                    element.ref().asType(),
                                    affected,
                                    checkedElementCombinations
                            ))
                        }
                    }
                }
            }
            when (affected) {
                is AttributesFor -> {
                    if (!affected.a.referencesType(LineAttributes::class.java)) {
                        continue@diffLoop
                    }

                    if (!affected.b.referencesType(GraphicObject::class.java)) {
                        continue@diffLoop
                    }

                    val graphicObject = modelRepository[affected.b]

                    if (graphicObject !is GraphicObject<*> || graphicObject.shape !is Line) {
                        continue@diffLoop
                    }

                    val goReference = graphicObject.ref().asType<GraphicObject<Line>>()

                    ret.mergeCollection(checkTransitionEndPointsForLine(
                            goReference,
                            modelRepository[affected.a.asType()],
                            checkedElementCombinations
                    ))
                }
            }
        }

        return ret
    }

    /**
     * Inserts / deletes the appropriate TransitionEndPoint for a StateElement that is represented by the GraphicObject
     * referenced by [graphicObjectRef].
     *
     * @param stateElement The StateElement for which [ConnectionEnd]s should be searched and applying
     *                     [TransitionEndPoint]s inserted.
     * @param graphicObjectRef Reference to the [GraphicObject] which is the representation for the [stateElement]
     * @param checkedElementCombinations The pairs of (ConnectionEnd, StateElement) IDs that have already been checked
     *                                   in the current run.
     *
     * @return Effective changes in the application model resulting from checking the TransitionEnd conditions.
     */
    private fun checkTransitionEndPointsOfState(
            stateElement: StateElement,
            graphicObjectRef: ElementReference<GraphicObject<*>>,
            checkedElementCombinations: MutableSet<Pair<String, String>>
    ): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val connectionEnds = modelRepository.getRelationsToElement(graphicObjectRef.id, ConnectionEnd::class.java)

        for ((_, end) in connectionEnds) {
            val checkedKey = Pair(end.id, stateElement.id)

            if (checkedKey in checkedElementCombinations) {
                continue
            }

            ret.mergeCollection(checkConnectionEnd(end, null, stateElement, checkedElementCombinations))
        }

        val existingTransitionEnds = modelRepository.getRelationsToElement(stateElement.id, TransitionEndPoint::class.java).values

        // remove old TransitionEnds (e.g. if the representation of a state has changed, but the ConnectionEnd to
        // the old representation remains)
        existingTransitionEnds.filter { it.a.id !in connectionEnds }.forEach {
            ret.mergeCollection(modelRepository.remove(it.id))
        }

        return ret
    }

    /**
     * Inserts / deletes the appropriate TransitionEndPoint for a changed Line [graphicObjectRef], given the
     * [attributes] set for the GraphicObject.
     *
     * @param attributes The LineAttributes set for the GraphicObject referenced by [graphicObjectRef].
     * @param graphicObjectRef A reference to a [Line] [GraphicObject] for which the TransitionEndpoints should be
     *                         checked.
     * @param checkedElementCombinations The pairs of (ConnectionEnd, StateElement) IDs that have already been checked
     *                                   in the current run.
     *
     * @return Effective changes in the application model resulting from checking the TransitionEnd conditions.
     */
    private fun checkTransitionEndPointsForLine(
            graphicObjectRef: ElementReference<GraphicObject<Line>>,
            attributes: LineAttributes?,
            checkedElementCombinations: MutableSet<Pair<String, String>>
    ): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val connectionEnds = modelRepository.getRelationsFromElement(graphicObjectRef.id, ConnectionEnd::class.java).values

        for (end in connectionEnds) {
            ret.mergeCollection(checkConnectionEnd(end, attributes, null, checkedElementCombinations))
        }

        return ret
    }

    /**
     * Inserts or removes appropriate [TransitionEndPoint] relations for the given [connectionEnd].
     *
     * A TransitionEndPoint is inserted if:
     * * A line has a [ConnectionEnd] relation to another GraphicObject
     * * That represents a [StateElement]
     * * The line is "solid", i.e. not dashed etc.
     * * The [TransitionEndPoint.Role] is defined by the connector end style of the line
     *
     * @param connectionEnd The connectionEnd to check.
     * @param givenAAttr If already known, the available [LineAttributes] for the [ConnectionEnd.a] line.
     *                   If <code>null</code> is given, the [LineAttributes] associated with the line will be fetched
     *                   via the [modelRepository].
     * @param givenBRepresented If already known, the [StateElement] represented by the [connectionEnd.b] GraphicObject.
     *                          If <code>null</code> is given, the [StateElement] is fetched via the
     *                          [RepresentsStateElement] Relation type.
     * @param checkedElementCombinations The pairs of (ConnectionEnd, StateElement) IDs that have already been checked
     *                                   in the current run.
     *
     */
    private fun checkConnectionEnd(
            connectionEnd: ConnectionEnd<*, *>,
            givenAAttr: LineAttributes?,
            givenBRepresented: StateElement?,
            checkedElementCombinations: MutableSet<Pair<String, String>>
    ): MutableTimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        val aAttr = givenAAttr ?: queries.getElementsRelatedTo(
                connectionEnd.a,
                AttributesFor::class.java,
                false
        ).filterIsInstance(LineAttributes::class.java).firstOrNull() ?: LineAttributes("tmp")

        val bRepresented = givenBRepresented ?: queries.getElementRelatedFrom(
                connectionEnd.b,
                RepresentsStateElement::class.java,
                false
        ) ?: return ret

        val containedKey = Pair(connectionEnd.id, bRepresented.id)

        if (containedKey in checkedElementCombinations) {
            return ret
        }

        val role = this.getTransitionEndRole(aAttr, connectionEnd.isEndConnection)

        // get existing TransitionEnd(s)
        val existing = modelRepository.getRelationsBetweenElements(
                connectionEnd.id,
                bRepresented.id,
                TransitionEndPoint::class.java
        ).toMutableMap()

        if (role != null) {
            val representation = existing.values.firstOrNull()?.also {
                it.probability = connectionEnd.probability
                it.role = role
            } ?: TransitionEndPoint(connectionEnd.ref(), bRepresented.ref(), connectionEnd.probability, role)

            ret.mergeCollection(modelRepository.store(representation))

            existing.remove(representation.id)
        }

        checkedElementCombinations.add(containedKey)

        existing.keys.forEach {
            ret.mergeCollection(modelRepository.remove(it))
        }

        return ret
    }

    /**
     * Based on the given [attributes], generates the [TransitionEndPoint.Role] for a [ConnectionEnd].
     *
     * @param attributes The attributes of the line that potentially represents a TransitionEnd
     * @param inspectEnd If set to true, the function will inspect the "End" connector style of the
     *                   Transition, else it will inspect the "start".
     */
    private fun getTransitionEndRole(attributes: LineAttributes, inspectEnd: Boolean): TransitionEndPoint.Role? {
        if (attributes.getLineStyle() != LineStyle.Option.SOLID) {
            return null
        }

        val connectorEndToInspect = if (inspectEnd) attributes.getEndStyle() else attributes.getStartStyle()

        return if (connectorEndToInspect == LineEndPointStyle.Option.NONE) {
            TransitionEndPoint.Role.FROM
        } else {
            TransitionEndPoint.Role.TO
        }
    }
}
