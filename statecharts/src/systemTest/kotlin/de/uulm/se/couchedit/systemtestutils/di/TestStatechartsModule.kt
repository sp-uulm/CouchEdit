package de.uulm.se.couchedit.systemtestutils.di

import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusStateCache
import de.uulm.se.couchedit.processing.compartment.controller.PotentialCompartmentDetector
import de.uulm.se.couchedit.processing.connection.controller.ConnectionEndDetector
import de.uulm.se.couchedit.processing.containment.controller.ContainmentProcessor
import de.uulm.se.couchedit.processing.spatial.controller.SpatialAbstractor
import de.uulm.se.couchedit.statecharts.di.StatechartsModule
import de.uulm.se.couchedit.statecharts.processing.AbstractSyntaxStoreProcessor
import de.uulm.se.couchedit.statecharts.processing.DisambiguationProcessor
import de.uulm.se.couchedit.statecharts.processing.hierarchy.StateCompartmentProcessor
import de.uulm.se.couchedit.statecharts.processing.hierarchy.StateHierarchyProcessor
import de.uulm.se.couchedit.statecharts.processing.labeling.StateLabelingProcessor
import de.uulm.se.couchedit.statecharts.processing.labeling.TransitionPotentialLabelProcessor
import de.uulm.se.couchedit.statecharts.processing.transition.TransitionEndProcessor
import de.uulm.se.couchedit.statecharts.processing.transition.TransitionProcessor
import de.uulm.se.couchedit.systemtestutils.controller.manager.TestModificationPortRegistry
import de.uulm.se.couchedit.systemtestutils.controller.modbus.TestModificationBusStateCacheImpl
import de.uulm.se.couchedit.systemtestutils.controller.modbus.factory.TestModificationBusManagerFactory
import de.uulm.se.couchedit.systemtestutils.controller.processing.TestProcessorModificationPortFactory

open class TestStatechartsModule(testModificationPortRegistry: TestModificationPortRegistry) : StatechartsModule() {
    public override val modificationPortFactory = TestProcessorModificationPortFactory(testModificationPortRegistry)
    override val modificationBusManagerFactory = TestModificationBusManagerFactory(testModificationPortRegistry)

    override fun bindModificationBusStateCache() {
        bind(ModificationBusStateCache::class.java).to(TestModificationBusStateCacheImpl::class.java)
    }

    override fun getProcessors(): Set<Class<out Processor>> {
        val coreProcessors = setOf(
                SpatialAbstractor::class.java,
                ConnectionEndDetector::class.java,
                //GenericConnectionSuggester::class.java,
                //CompositionSuggester::class.java,

                //PrintlnProcessor::class.java,
                //TestSuggestionProcessor::class.java,
                //TestHotSpotProcessor::class.java,
                PotentialCompartmentDetector::class.java,
                ContainmentProcessor::class.java
        )

        val statechartProcessors = setOf(
                StateHierarchyProcessor::class.java,
                StateLabelingProcessor::class.java,
                TransitionEndProcessor::class.java,
                TransitionProcessor::class.java,
                TransitionPotentialLabelProcessor::class.java,
                AbstractSyntaxStoreProcessor::class.java,
                StateCompartmentProcessor::class.java,
                DisambiguationProcessor::class.java
        )

        return coreProcessors.union(statechartProcessors)
    }
}
