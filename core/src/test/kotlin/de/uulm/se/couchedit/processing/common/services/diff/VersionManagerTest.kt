package de.uulm.se.couchedit.processing.common.services.diff

import de.uulm.se.couchedit.processing.common.model.time.VectorTimestamp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VersionManagerTest {
    private val systemUnderTestId = "VersionManager_in_testing"

    private val systemUnderTest = VersionManager(systemUnderTestId)

    @BeforeEach
    fun setUp() {
        systemUnderTest.updateVersion("start", VectorTimestamp(mutableMapOf(
                "a" to 42L,
                "b" to 1337L,
                "x" to 11111L
        )))
    }

    @Nested
    inner class RegisterLocalEvent {
        @Test
        fun `Elements updated after the call should be causally dependent on Elements updated before the call`() {
            val timestampBefore = systemUnderTest.markElementUpdated("test")


            systemUnderTest.registerLocalEvent()


            val timestampAfter = systemUnderTest.markElementUpdated("test")

            assert(timestampAfter.relationTo(timestampBefore) == VectorTimestamp.CausalRelation.STRICTLY_AFTER)
        }

        @Test
        fun `should not have influence on timestamps given externally`() {
            val timestampToInsert = VectorTimestamp(mutableMapOf("1" to 1L, "2" to 2L))

            val id1 = "test"
            val id2 = "test2"


            systemUnderTest.updateVersion(id1, timestampToInsert)
            systemUnderTest.registerLocalEvent()
            systemUnderTest.updateVersion(id2, timestampToInsert)


            assertThat(systemUnderTest.versionOf(id1)).isEqualTo(systemUnderTest.versionOf(id2))
        }

        @Test
        fun `should not have an influence on timestamps already stored`() {
            val id = "test"

            systemUnderTest.markElementUpdated(id)
            val timestampBefore = systemUnderTest.versionOf(id)


            systemUnderTest.registerLocalEvent()
            val timestampAfter = systemUnderTest.versionOf(id)


            assertThat(timestampAfter).isEqualTo(timestampBefore)
        }
    }

    @Nested
    inner class MarkElementUpdated {
        @Test
        fun `value should be returned in subsequent versionOf calls if accepted`() {
            val id = "test"

            val testTimestampKey = "testKey"

            val timestamp = VectorTimestamp(mutableMapOf(testTimestampKey to 99L))


            val result = systemUnderTest.updateVersion(id, timestamp)
            val newVersionOf = systemUnderTest.versionOf(id)!!


            assertThat(result).isTrue()

            assertThat(newVersionOf[testTimestampKey]).isEqualTo(timestamp[testTimestampKey])
        }

        @Test
        fun `value should be rejected if it is before the old stored version`() {
            val id = "test"

            val testTimestampKey = "testKey"

            val timestamp = VectorTimestamp(mutableMapOf(testTimestampKey to 666L))
            val olderTimestamp = VectorTimestamp(mutableMapOf(testTimestampKey to 333L))

            val result = systemUnderTest.updateVersion(id, timestamp)
            val result2 = systemUnderTest.updateVersion(id, olderTimestamp)

            val newTimestamp = systemUnderTest.versionOf(id)!!

            assertThat(result).isTrue()
            assertThat(result2).isFalse()

            assertThat(newTimestamp[testTimestampKey]).isEqualTo(timestamp[testTimestampKey])
        }

        @Test
        fun `should throw IllegalStateException if trying to update the value of VersionManager's own timestamp`() {
            systemUnderTest.registerLocalEvent()
            systemUnderTest.registerLocalEvent()

            val currentTimestamp = systemUnderTest.markElementUpdated("test")

            val newTimestamp = VectorTimestamp(mutableMapOf(
                    systemUnderTestId to currentTimestamp[systemUnderTestId] + 1
            ))

            assertThatIllegalStateException().isThrownBy {
                systemUnderTest.updateVersion("test", newTimestamp)
            }
        }
    }

    @Nested
    inner class OnRemove {
        @Test
        fun `should make the VersionManager return null on subsequent versionOf calls`() {
            val id = "test"
            systemUnderTest.updateVersion(id, VectorTimestamp(mutableMapOf(
                    "abc" to 95L
            )))
            systemUnderTest.onRemove(id)


            val newTimestamp = systemUnderTest.versionOf(id)


            assertThat(newTimestamp).isNull()
        }
    }
}
