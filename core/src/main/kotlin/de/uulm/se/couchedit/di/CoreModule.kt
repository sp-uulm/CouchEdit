package de.uulm.se.couchedit.di

import com.google.common.reflect.TypeToken
import com.google.inject.*
import com.google.inject.multibindings.MapBinder
import com.google.inject.name.Named
import com.google.inject.name.Names
import de.uulm.se.couchedit.debug.PrintlnProcessor
import de.uulm.se.couchedit.debug.TestSuggestionProcessor
import de.uulm.se.couchedit.di.scope.SimpleScope
import de.uulm.se.couchedit.di.scope.processor.ProcessorScoped
import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.ElementReference
import de.uulm.se.couchedit.model.hotspot.HotSpotDefinition
import de.uulm.se.couchedit.processing.attribute.factory.SubAttributeBagFactory
import de.uulm.se.couchedit.processing.common.controller.ModificationPort
import de.uulm.se.couchedit.processing.common.controller.Processor
import de.uulm.se.couchedit.processing.common.controller.factory.ProcessorModificationPortFactory
import de.uulm.se.couchedit.processing.common.controller.factory.ProductionProcessorModificationPortFactory
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusStateCache
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusStateCacheImpl
import de.uulm.se.couchedit.processing.common.modbus.factory.ModificationBusManagerFactory
import de.uulm.se.couchedit.processing.common.modbus.factory.ProductionModificationBusManagerFactory
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.child.ChildModelRepositoryFactory
import de.uulm.se.couchedit.processing.common.repository.child.graph.GraphChildModelRepositoryFactory
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.services.datastore.IdGenerator
import de.uulm.se.couchedit.processing.common.services.datastore.LinearIDGenerator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.common.services.diff.VersionManager
import de.uulm.se.couchedit.processing.compartment.controller.PotentialCompartmentDetector
import de.uulm.se.couchedit.processing.compartment.services.CompartmentHotSpotProvider
import de.uulm.se.couchedit.processing.connection.controller.ConnectionEndDetector
import de.uulm.se.couchedit.processing.containment.controller.ContainmentProcessor
import de.uulm.se.couchedit.processing.graphic.attribute.factory.GraphicAttributeBagFactory
import de.uulm.se.couchedit.processing.graphic.composition.controller.CompositionSuggester
import de.uulm.se.couchedit.processing.graphic.services.LineFractionHotSpotProvider
import de.uulm.se.couchedit.processing.hotspot.services.HotSpotProvider
import de.uulm.se.couchedit.processing.hotspot.services.TestHotSpotProvider
import de.uulm.se.couchedit.processing.spatial.controller.SpatialAbstractor
import de.uulm.se.couchedit.processing.spatial.services.geometric.DelegatingShapeExtractor
import de.uulm.se.couchedit.processing.spatial.services.geometric.JTSShapeBoundsCalculator
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeBoundsCalculator
import de.uulm.se.couchedit.processing.spatial.services.geometric.ShapeExtractor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.javaMethod

/**
 * Contains the main configuration for the application's core processing system.
 * Language-specific components should subclass this module and extend it with their own facilities
 * (TODO: find a way for more dynamic discovery of sub-components)
 */
open class CoreModule : AbstractModule() {
    protected open val modificationPortFactory: ProcessorModificationPortFactory = ProductionProcessorModificationPortFactory()
    protected open val modificationBusManagerFactory: ModificationBusManagerFactory = ProductionModificationBusManagerFactory()

    override fun configure() {
        createProcessorScope()

        bindSpatialServices()

        bindHotSpotProviders()
        bindAttributeBagFactories()

        bindModelRepository()

        bind(IdGenerator::class.java).to(LinearIDGenerator::class.java)

        bindModificationBusStateCache()
    }

    protected open fun bindModificationBusStateCache() {
        bind(ModificationBusStateCache::class.java).to(ModificationBusStateCacheImpl::class.java)
    }

    @Provides
    @Singleton
    @Named("centralExecutor")
    fun provideCentralExecutor(): ExecutorService {
        val threadNumber = Runtime.getRuntime().availableProcessors()

        val ret = ThreadPoolExecutor(threadNumber, threadNumber, 60, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

        ret.allowCoreThreadTimeOut(true)

        return ret
    }

    @Provides
    @Singleton
    fun provideProcessorManager(injector: Injector): ModificationBusManager {
        val processors = getProcessors()

        val processorScope = injector.scopeBindings[ProcessorScoped::class.java] as SimpleScope

        processorScope.enter()

        val cacheVersionManager = VersionManager("_ModificationBusStateCache")

        processorScope.seed(VersionManager::class.java, cacheVersionManager)

        val diffCache = injector.getInstance(ModificationBusStateCache::class.java)

        processorScope.exit()

        val modificationPorts = processors.map {
            processorScope.enter()

            val versionManager = VersionManager(it.simpleName)

            processorScope.seed(VersionManager::class.java, versionManager)

            val instance = modificationPortFactory.createProcessorModificationPort(
                    injector.getInstance(it),
                    injector.getInstance(DiffCollectionFactory::class.java),
                    injector.getInstance(Key.get(ExecutorService::class.java, Names.named("centralExecutor")))
            )

            processorScope.exit()

            return@map instance
        }.toMutableSet()


        for (key in injector.allBindings.keys) {
            if (ModificationPort::class.java.isAssignableFrom(key.typeLiteral.rawType)) {
                val implementation = injector.getInstance(key) as ModificationPort
                modificationPorts.add(implementation)
            }
        }

        return modificationBusManagerFactory.createModificationBusManager(modificationPorts, diffCache)
    }

    private fun createProcessorScope() {
        val processorScope = SimpleScope()

        bindScope(ProcessorScoped::class.java, processorScope)

        bind(SimpleScope::class.java).annotatedWith(Names.named("processorScope")).toInstance(processorScope)
    }

    /**
     * Binds a [MapBinder] from [HotSpotDefinition] to [HotSpotProvider] implementations given by the [hotSpotProviders]
     * constant
     *
     * @todo Make this extensible by external modules, use
     *       [ServiceLoader](https://docs.oracle.com/javase/9/docs/api/java/util/ServiceLoader.html)??
     */
    private fun bindHotSpotProviders() {
        val tlHotSpotDefinition = object : TypeLiteral<Class<HotSpotDefinition<*>>>() {}
        val tlHotSpotProvider = object : TypeLiteral<HotSpotProvider<*, *>>() {}

        val mapBinder = MapBinder.newMapBinder(binder(), tlHotSpotDefinition, tlHotSpotProvider)

        for (provider in getHotSpotProviders()) {
            val typeToken = TypeToken.of(provider)

            // Use Guava TypeToken to find out which type of
            val generateShapeMethod = typeToken.method(HotSpotProvider<*, *>::generateShape.javaMethod!!)

            val elementRefToken = generateShapeMethod.parameters[0].type
            val elementType = elementRefToken.resolveType(ElementReference::class.java.typeParameters[0]).rawType

            // Type of consumed HotSpotDefinition is the second type parameter.
            // Suppressed as constraint of type parameter in HotSpotProvider ensures
            // that this is always a Class<HotSpotDefinition>
            @Suppress("UNCHECKED_CAST")
            (elementType as Class<HotSpotDefinition<*>>).let {
                mapBinder.addBinding(it).to(provider)
            }
        }
    }

    /**
     * Binds a [MapBinder] from [HotSpotDefinition] to [HotSpotProvider] implementations given by the [hotSpotProviders]
     * constant
     *
     * @todo Make this extensible by external modules, use
     *       [ServiceLoader](https://docs.oracle.com/javase/9/docs/api/java/util/ServiceLoader.html)??
     * @todo Generic way for doing this (together with bindHotSpotProviders)
     */
    private fun bindAttributeBagFactories() {
        val tlElement = object : TypeLiteral<Class<Element>>() {}
        val tlMapBinderFactory = object : TypeLiteral<SubAttributeBagFactory<*>>() {}

        val mapBinder = MapBinder.newMapBinder(binder(), tlElement, tlMapBinderFactory)

        for (factory in getAttributeBagFactories()) {
            val factoryToken = TypeToken.of(factory)

            val bagTypesMethod = factoryToken.method(SubAttributeBagFactory<*>::availableBagTypes.javaMethod!!)

            val elementType = bagTypesMethod.parameters[0].type.rawType

            // Type of consumed element is the first type parameter of the SubAttributeFactoryInterface
            @Suppress("UNCHECKED_CAST")
            (elementType as Class<Element>).let {
                mapBinder.addBinding(it).to(factory)
            }
        }
    }

    /**
     * Binds the implementations for [ModelRepository] and associated classes.
     */
    private fun bindModelRepository() {
        bind(ModelRepository::class.java).to(RootGraphBasedModelRepository::class.java)

        bind(ChildModelRepositoryFactory::class.java).to(GraphChildModelRepositoryFactory::class.java)
    }

    private fun bindSpatialServices() {
        // this interface is necessary for Guice to be able to proxy circular dependencies
        bind(ShapeExtractor::class.java).to(DelegatingShapeExtractor::class.java)

        bind(ShapeBoundsCalculator::class.java).to(JTSShapeBoundsCalculator::class.java)
    }

    /**
     * Subclasses should return a set of [HotSpotProvider] classes which will be used to produce HotSpot shapes when
     * queried by the [ShapeExtractor].
     */
    protected open fun getHotSpotProviders(): Set<Class<out HotSpotProvider<*, *>>> = setOf(
            TestHotSpotProvider::class.java,
            CompartmentHotSpotProvider::class.java,
            LineFractionHotSpotProvider::class.java
    )

    /**
     * Subclasses should return a set of [SubAttributeBagFactory] which will be used to produce AttributeBags when
     * queried by the AggregateAttributeBagFactory.
     */
    protected open fun getAttributeBagFactories(): Set<Class<out SubAttributeBagFactory<*>>> = setOf(
            GraphicAttributeBagFactory::class.java
    )

    /**
     * Subclasses should return a set of [Processor]s which will be autoconfigured by the Module.
     */
    protected open fun getProcessors(): Set<Class<out Processor>> = setOf(
            SpatialAbstractor::class.java,
            ConnectionEndDetector::class.java,
            //GenericConnectionSuggester::class.java,
            CompositionSuggester::class.java,

            PrintlnProcessor::class.java,
            TestSuggestionProcessor::class.java,
            //TestHotSpotProcessor::class.java,
            PotentialCompartmentDetector::class.java,
            ContainmentProcessor::class.java
    )
}
