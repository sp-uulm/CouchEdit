package de.uulm.se.couchedit.testsuiteutils.controller

import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.model.TestStepInfo
import de.uulm.se.couchedit.testsuiteutils.property.DestructibleLazyManager
import org.junit.jupiter.api.AfterAll
import kotlin.reflect.KFunction
import kotlin.system.measureNanoTime

/**
 * Test case that is run within a [CouchEditTestSuite]. This allows for parameterizing entire test classes (scenarios).
 *
 * The tests are executed as if they were annotated with PER_CLASS lifecycle and OrderAnnotation order.
 */
abstract class CouchEditTest {
    private val lazyManager = DestructibleLazyManager()

    /**
     * The TestSuite to which the results of [measure] calls should be forwarded
     */
    var suite: CouchEditTestSuite? = null

    /**
     * Information about the TestCase.
     */
    abstract val testInstanceInfo: TestInstanceInfo

    protected fun <T> disposableLazy(initializer: () -> T): DestructibleLazyManager.DelegateProvider<T> {
        return lazyManager.getLazy(initializer)
    }

    /**
     * Run the given [toExecute] function, measure the time it takes, and then print + save the metadata of the operation.
     */
    fun <R> t(
            action: Action,
            inputDiffCollectionSize: Int,
            method: KFunction<*>,
            description: String? = null,
            additionalInfo: Map<String, Any> = emptyMap(),
            toExecute: () -> R
    ): R {
        val orderNumber = TestMethodUtil.getMethodOrderNumber(method)

        var result: R? = null

        val nanoseconds = measureNanoTime {
            result = toExecute()
        }

        val milliseconds = nanoseconds.toDouble() / 1000000

        val orderNumberString = orderNumber?.toString()?.let { "$it " } ?: ""

        println("$orderNumberString - $action - $inputDiffCollectionSize diffs - $description: $milliseconds ms")

        val testStepInfo = TestStepInfo(orderNumber, action, method, inputDiffCollectionSize, milliseconds, additionalInfo)
        suite?.addStepInfo(this, testInstanceInfo, testStepInfo)

        return result!!
    }

    @AfterAll
    fun teardown() {
        lazyManager.destroyAll()
        System.gc()
    }

    /**
     * Describes the action undertaken during the given measure block
     */
    enum class Action {
        PROCESS,
        APPLY
    }
}
