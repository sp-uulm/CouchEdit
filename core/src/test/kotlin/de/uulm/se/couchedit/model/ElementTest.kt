package de.uulm.se.couchedit.model

import de.uulm.se.couchedit.model.base.Element
import de.uulm.se.couchedit.model.base.probability.ProbabilityInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.KProperty1

@Suppress("UsePropertyAccessSyntax") // following this lint for AssertJ leads to non-compiling code
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ElementTest<E : Element> {
    /**
     * Implementations must return sets of [E] Elements here where:
     * * In each set, all instances are equivalent
     * * Between all sets, no instances are allowed to be equivalent.
     */
    abstract fun getSetsOfMutuallyEquivalentInstances(): Set<Set<E>>

    abstract fun getContentRelevantProperties(): Set<KProperty1<in E, *>>

    /**
     * Pairs of instances that are mutually unequivalent according to the contentEquivalent method.
     */
    protected lateinit var contentUnequivalentInstancePairs: Set<Pair<E, E>>

    @BeforeAll
    fun check() {
        val relevantProperties = getContentRelevantProperties()

        val instances = getSetsOfMutuallyEquivalentInstances()

        val newInstancePairs = mutableSetOf<Pair<E, E>>()

        for (instanceSet in instances) {
            for (instance1 in instanceSet) {
                for (instanceSet2 in instances) {
                    if (instanceSet === instanceSet2) {
                        continue
                    }

                    for (instance2 in instanceSet2) {
                        newInstancePairs.add(Pair(instance1, instance2))

                        val anyPropertyDifferent = relevantProperties.any { property ->
                            val instance1Value = property.get(instance1)
                            val instance2Value = property.get(instance2)

                            return@any instance1Value != instance2Value
                        }

                        if (!anyPropertyDifferent) {
                            throw RuntimeException("At least one content relevant property must be different for " +
                                    "$instance1 and $instance2 to be correctly unequivalent")
                        }
                    }
                }
            }
        }

        contentUnequivalentInstancePairs = newInstancePairs

    }

    @Nested
    inner class Equivalent {
        @Test
        fun `should return true for all instances marked as content-equivalent if the probability is also the same`() {
            val instances = getSetsOfMutuallyEquivalentInstances()

            for (instanceList in instances) {
                for (instance1 in instanceList) {
                    for (instance2 in instanceList) {
                        if (instance1 === instance2) {
                            continue
                        }

                        if (instance1.probability != instance2.probability) {
                            continue
                        }

                        assertThat(instance1.equivalent(instance2)).describedAs("$instance1 and $instance2 were expected " +
                                "to be equivalent, but were not").isTrue()
                    }
                }
            }
        }

        @Test
        fun `should return false for copied instances which had their probability changed`() {
            assertForAllInstances {
                val copied = it.copy()

                switchProbability(copied)

                assertThat(copied.equivalent(it))
            }
        }

        @Test
        fun `should return false for all instances marked as content-unequivalent`() {
            for ((instance1, instance2) in contentUnequivalentInstancePairs) {
                assertThat(instance1.equivalent(instance2)).describedAs("$instance1 and $instance2 were expected to be " +
                        "not equivalent, but equivalent() returned true").isFalse()
            }
        }
    }

    @Nested
    inner class ContentEquivalent() {
        @Test
        fun `should return true for all instances marked as content-equivalent`() {
            val instances = getSetsOfMutuallyEquivalentInstances()

            for (instanceList in instances) {
                for (instance1 in instanceList) {
                    for (instance2 in instanceList) {
                        if (instance1 === instance2) {
                            continue
                        }

                        assertThat(instance1.contentEquivalent(instance2))
                                .describedAs("$instance1 and $instance2 were expected to be equivalent, but were not")
                                .isTrue()
                    }
                }
            }
        }

        @Test
        fun `should still return true for copied instances which had their probability changed`() {
            assertForAllInstances {
                val copied = it.copy()

                if (copied.probability == ProbabilityInfo.Explicit) {
                    copied.probability = ProbabilityInfo.Generated(0.9)
                } else if (copied.probability is ProbabilityInfo.Generated) {
                    copied.probability = ProbabilityInfo.Explicit
                }

                assertThat(copied.contentEquivalent(it)).isTrue()
            }
        }

        @Test
        fun `should return false for all instances marked as content-unequivalent`() {
            for ((instance1, instance2) in contentUnequivalentInstancePairs) {
                assertThat(instance1.equivalent(instance2))
                        .describedAs("$instance1 and $instance2 were expected to be not equivalent, but equivalent() returned true")
                        .isFalse()
            }
        }
    }


    @Nested
    inner class Copy {
        @Test
        fun `should return a different instance than the original`() {
            assertForAllInstances {
                val copied = it.copy()

                assertThat(copied).isNotSameAs(it)
            }
        }

        @Test
        fun `should return an instance equivalent to the original`() {
            assertForAllInstances {
                val copied = it.copy()

                assertThat(copied.equivalent(it)).isTrue()
                assertThat(it.equivalent(copied)).isTrue()
            }
        }
    }

    private fun assertForAllInstances(assertion: (E) -> Unit) {
        val instances = getSetsOfMutuallyEquivalentInstances().flatten()

        for (instance in instances) {
            assertion(instance)
        }
    }

    private fun switchProbability(element: Element) {
        if (element.probability == ProbabilityInfo.Explicit) {
            element.probability = ProbabilityInfo.Generated(0.9)
        } else if (element.probability is ProbabilityInfo.Generated) {
            element.probability = ProbabilityInfo.Explicit
        }
    }
}
