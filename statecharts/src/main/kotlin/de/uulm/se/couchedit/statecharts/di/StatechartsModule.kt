package de.uulm.se.couchedit.statecharts.di

import de.uulm.se.couchedit.di.CoreModule
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.controller.factory.ProcessorModificationPortFactory
import de.uulm.se.couchedit.processing.common.modbus.factory.ModificationBusManagerFactory
import de.uulm.se.couchedit.statecharts.processing.AbstractSyntaxStoreProcessor
import de.uulm.se.couchedit.statecharts.processing.DisambiguationProcessor
import de.uulm.se.couchedit.statecharts.processing.hierarchy.StateCompartmentProcessor
import de.uulm.se.couchedit.statecharts.processing.hierarchy.StateHierarchyProcessor
import de.uulm.se.couchedit.statecharts.processing.labeling.StateLabelingProcessor
import de.uulm.se.couchedit.statecharts.processing.labeling.TransitionPotentialLabelProcessor
import de.uulm.se.couchedit.statecharts.processing.transition.TransitionEndProcessor
import de.uulm.se.couchedit.statecharts.processing.transition.TransitionProcessor

open class StatechartsModule : CoreModule() {
    override fun getProcessors(): Set<Class<out Processor>> {
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

        return super.getProcessors().union(statechartProcessors)
    }
}
