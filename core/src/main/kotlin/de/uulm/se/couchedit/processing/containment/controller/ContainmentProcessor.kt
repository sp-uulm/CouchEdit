package de.uulm.se.couchedit.processing.containment.controller

import com.google.inject.Inject
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.containment.Contains
import de.uulm.se.couchedit.model.graphic.ShapedElement
import de.uulm.se.couchedit.model.graphic.elements.GraphicObject
import de.uulm.se.couchedit.model.spatial.relations.Include
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.child.graph.GraphChildModelRepositoryFactory
import de.uulm.se.couchedit.processing.common.repository.child.specification.childRepoSpec
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory

/**
 * A [Processor] generating the transitive reduction of the tree of GraphicObjects, HotSpotDefinitions and
 * Include relations.
 *
 * This can be used by downstream processors to generate the "real" hierarchy of Elements on top of each other
 * without needing to traverse redundant Includes.
 */
@ProcessorScoped
class ContainmentProcessor @Inject constructor(
        private val modelRepository: ModelRepository,
        private val diffCollectionFactory: DiffCollectionFactory,
        private val applicator: Applicator,
        childModelRepositoryFactory: GraphChildModelRepositoryFactory
) : Processor {
    private val includeToContainsMap = mutableMapOf<String, String>()

    private val childModelRepository = childModelRepositoryFactory.provideChildRepository(
            modelRepository,
            childRepoSpec {
                classes {
                    setToExclude()
                    // Exclude Contains. By the filtering of the [consumes] method, thus only ShapedElements
                    // (including HotSpotDefinition relations) and Include relations remain.
                    this += Contains::class.java
                }

                relationsWithEndpoints(Include::class.java) {
                    setToInclude()

                    // Only consider Include relations which point to a GraphicObject and not another ShapedElement
                    addBType(GraphicObject::class.java)
                }

                transitiveReduction(Include::class.java)
            }
    )

    override fun consumes(): List<Class<out Element>> = listOf(
            ShapedElement::class.java,
            Include::class.java
    )

    override fun process(diffs: TimedDiffCollection): TimedDiffCollection {
        val ret = diffCollectionFactory.createMutableTimedDiffCollection()

        // add changes
        applicator.apply(diffs)

        // collect the Contains relations needed
        val toRemove = includeToContainsMap.toMutableMap()

        for ((id, includeRelation) in this.childModelRepository.getAll(Include::class.java)) {
            if (id in toRemove) {
                // There is already a Contains relation for this, nothing to do
                toRemove.remove(id)
                continue
            }

            // else create a new Contains relation
            val containsRelation = Contains(includeRelation.a, includeRelation.b.asType())

            ret.mergeCollection(this.modelRepository.store(containsRelation))

            this.includeToContainsMap[id] = containsRelation.id
        }

        // remove all obsolete Contains relations
        for ((includeId, containsId) in toRemove) {
            ret.mergeCollection(modelRepository.remove(containsId))

            this.includeToContainsMap.remove(includeId)
        }

        return ret
    }
}
