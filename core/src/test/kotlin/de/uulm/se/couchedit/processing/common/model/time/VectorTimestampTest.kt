package de.uulm.se.couchedit.processing.common.model.time

import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp.CausalRelation.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VectorTimestampTest {
    private val systemUnderTest = VectorTimestamp(mutableMapOf(
            "A" to 5L,
            "B" to -5L,
            "C" to 5L
    ))

    @Nested
    inner class RelationTo {
        @Nested
        inner class EQUAL_cases {
            @Test
            fun `timestamp with exactly the same components`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 5L,
                        "B" to -5L,
                        "C" to 5L
                ))


                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(EQUAL)
            }
        }


        @Nested
        inner class STRICTLY_BEFORE_cases {
            @Test
            fun `compared to a timestamp with some components bigger`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 6L,
                        "B" to -5L,
                        "C" to 5L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(STRICTLY_BEFORE)
            }

            @Test
            fun `compared to a timestamp with extra components`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 5L,
                        "B" to -5L,
                        "C" to 5L,
                        "D" to 2L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(STRICTLY_BEFORE)
            }

            @Test
            fun `compared to a timestamp with negative component missing`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 5L,
                        "C" to 5L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(STRICTLY_BEFORE)
            }
        }

        @Nested
        inner class STRICTLY_AFTER_cases {
            @Test
            fun `compared to a timestamp with a component smaller`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 4L,
                        "B" to -5L,
                        "C" to 5L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(STRICTLY_AFTER)
            }

            @Test
            fun `compared to a timestamp with component missing`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 5L,
                        "B" to -5L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(STRICTLY_AFTER)
            }

            @Test
            fun `compared to a timestamp with negative extra components`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 5L,
                        "B" to -5L,
                        "C" to 5L,
                        "D" to -2L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(STRICTLY_AFTER)
            }
        }

        @Nested
        inner class PARALLEL_cases {
            @Test
            fun `compared to a timestamp with some components bigger and some smaller`() {
                val input = VectorTimestamp(mutableMapOf(
                        "A" to 4L,
                        "B" to -5L,
                        "C" to 6L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(PARALLEL)
            }

            @Test
            fun `compared to a timestamp with some extra components bigger and some components missing`() {
                val input = VectorTimestamp(mutableMapOf(
                        "B" to -5L,
                        "C" to 6L,
                        "D" to 2L
                ))

                val result = systemUnderTest.relationTo(input)


                assertThat(result).isEqualTo(PARALLEL)
            }
        }
    }
}
