package de.uulm.se.couchedit.systemtestutils.test

import com.google.inject.Guice
import com.google.inject.Key
import com.google.inject.name.Names
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import de.uulm.se.couchedit.processing.common.model.diffcollection.TimedDiffCollection
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.systemtestutils.controller.manager.TestModificationPortRegistry
import de.uulm.se.couchedit.systemtestutils.controller.processing.model.TrackableDiffCollectionWrapper
import de.uulm.se.couchedit.systemtestutils.di.TestStatechartsModule
import de.uulm.se.couchedit.testsuiteutils.TestUtilityConstants
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import de.uulm.se.couchedit.testsuiteutils.controller.TestMethodUtil
import de.uulm.se.couchedit.testsuiteutils.model.TestStepInfo
import org.junit.jupiter.api.BeforeAll
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import kotlin.reflect.KFunction

/**
 * Base class for whole-system tests of the CouchEdit Statechart configuration.
 *
 * This class sets up a [ModificationBusManager] that is bootstrapped by the [StateChartModule]
 */
abstract class BaseSystemTest : CouchEditTest() {
    protected val registry by disposableLazy {
        TestModificationPortRegistry()
    }

    protected val module by disposableLazy {
        TestStatechartsModule(registry)
    }

    protected val injector by disposableLazy { Guice.createInjector(module) }

    protected abstract val systemTestProcessor: SystemTestProcessor

    protected val testApplicator by lazy { systemTestProcessor.applicator }

    protected val testModelRepository by lazy { systemTestProcessor.modelRepository }

    protected val testDiffCollectionFactory by lazy { injector.getInstance(DiffCollectionFactory::class.java) }

    protected val modPort by disposableLazy {
        SystemTestModificationPort()
    }

    protected val modificationBusManager by disposableLazy {
        return@disposableLazy injector.getInstance(ModificationBusManager::class.java)
    }

    @BeforeAll
    fun setUp() {
        modificationBusManager.registerModificationPort(module.modificationPortFactory.createProcessorModificationPort(
                systemTestProcessor,
                injector.getInstance(DiffCollectionFactory::class.java),
                injector.getInstance(Key.get(ExecutorService::class.java, Names.named("centralExecutor")))
        ))

        modificationBusManager.registerModificationPort(modPort)
    }

    fun pt(diffCollection: TimedDiffCollection, method: KFunction<*>, description: String) {
        val orderNumber = TestMethodUtil.getMethodOrderNumber(method)

        val nanosecondsPrev = System.nanoTime()

        val future = CompletableFuture<Map<String, Double>>()

        val wrapper = TrackableDiffCollectionWrapper(setOf("_input"), diffCollection)

        registry.setCompletableFuture(future, wrapper.ids)

        this.modPort.publish(wrapper)

        val times = future.get().mapKeys { (k, _) ->
            TestUtilityConstants.RESULT_PREFIX + k
        }

        val nanoseconds = System.nanoTime() - nanosecondsPrev
        val milliseconds = nanoseconds.toDouble() / 1000000

        println("$orderNumber - PROCESSING TIME - ${nanoseconds / 1000000} ms")

        val testStepInfo = TestStepInfo(
                orderNumber,
                Action.PROCESS,
                method,
                diffCollection.size,
                milliseconds,
                times
        )

        suite?.addStepInfo(this, testInstanceInfo, testStepInfo)
    }
}
