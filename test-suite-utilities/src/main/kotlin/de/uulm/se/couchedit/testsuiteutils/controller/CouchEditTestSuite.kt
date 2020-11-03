package de.uulm.se.couchedit.testsuiteutils.controller

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import de.uulm.se.couchedit.testsuiteutils.annotation.CouchEditSuiteTest
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.model.TestStepInfo
import de.uulm.se.couchedit.testsuiteutils.model.WrappedTestInstanceInfo
import org.junit.jupiter.api.*
import java.lang.reflect.Method
import java.time.Duration
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Suite of [CouchEditTests] which will be executed using reflection and the DynamicTest API of JUnit5.
 *
 * This currently only has support for a very limited subset of
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CouchEditTestSuite {
    private val producedInfos = HashMultimap.create<WrappedTestInstanceInfo, TestStepInfo>()

    /**
     * Test Instances that should be run to initialize the system before performance testing takes place.
     */
    protected open fun getDryRunTestInstances(): List<CouchEditTest> = emptyList()

    protected abstract fun getTestInstances(): List<CouchEditTest>

    fun addStepInfo(testCase: CouchEditTest, testInstanceInfo: TestInstanceInfo, testStepInfo: TestStepInfo) {
        producedInfos.put(WrappedTestInstanceInfo(testCase::class.java, testInstanceInfo), testStepInfo)
    }

    @TestFactory
    @DisplayName("Tests")
    fun getTests(): List<DynamicNode> {
        val dryRunInstances = getDryRunTestInstances()
        val testInstances = getTestInstances()

        val containers = mutableListOf<DynamicContainer>()

        for (dryRunInstance in dryRunInstances) {
            addToTestExecution(dryRunInstance, containers, "DRY RUN - ")
        }

        for (testInstance in testInstances) {
            testInstance.suite = this

            addToTestExecution(testInstance, containers, "")
        }

        return containers
    }

    @AfterAll
    fun exportResults() {
        TestOutputWriter.write(this::class.java.simpleName, producedInfos)
    }

    private fun addToTestExecution(
            testInstance: CouchEditTest,
            containers: MutableList<DynamicContainer>,
            displayNamePrefix: String
    ) {

        val displayName = testInstance.testInstanceInfo.displayName
        val classMethods = testInstance::class.java.methods

        val beforeAllNode = DynamicTest.dynamicTest("BeforeAll") {
            classMethods.filter { it.getAnnotation(BeforeAll::class.java) != null }.forEach { it.invoke(testInstance) }
        }

        val testDynamicNodes = getDynamicTestsWithOrder(classMethods, testInstance)
                .entries()
                .sortedBy(MutableMap.MutableEntry<Int, DynamicNode>::key)
                .map(MutableMap.MutableEntry<Int, DynamicNode>::value)

        val afterAllNode = DynamicTest.dynamicTest("AfterAll") {
            classMethods.filter { it.getAnnotation(AfterAll::class.java) != null }.forEach { it.invoke(testInstance) }
        }

        val container = DynamicContainer.dynamicContainer(
                displayNamePrefix + displayName,
                listOf(beforeAllNode) + testDynamicNodes + afterAllNode
        )
        containers.add(container)
    }

    private fun getDynamicTestsWithOrder(classMethods: Array<out Method>, testInstance: CouchEditTest): Multimap<Int, DynamicNode> {
        val ret = HashMultimap.create<Int, DynamicNode>()

        for (method in classMethods) {
            val order = method.getAnnotation(Order::class.java)?.value ?: 0

            if (method.getAnnotation(CouchEditSuiteTest::class.java) != null) {
                ret.put(order, DynamicTest.dynamicTest(method.name) {
                    println("START TEST ${testInstance.testInstanceInfo.displayName} - ${method.name}")

                    try {
                        assertTimeout(Duration.ofMinutes(5)) {
                            method.invoke(testInstance)
                        }
                    } catch (e: Exception) {
                        println("TEST FAIL ${testInstance.testInstanceInfo.displayName} - ${method.name}")

                        throw e
                    }
                })
            } else if (method.getAnnotation(TestFactory::class.java) != null) {
                val dynTests = method.invoke(testInstance)

                val dynTestsIterable = when (dynTests) {
                    is Stream<*> -> dynTests.toList()
                    is Iterable<*> -> dynTests
                    else -> throw IllegalArgumentException("Currently cannot use ${dynTests::class.java}")
                }.filterIsInstance<DynamicNode>()

                ret.put(order, DynamicContainer.dynamicContainer(method.name, dynTestsIterable))
            }
        }

        return ret
    }
}
