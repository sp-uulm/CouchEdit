package de.uulm.se.couchedit.testsuiteutils.model

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import kotlin.reflect.KFunction

data class TestStepInfo(
        val stepNumber: Int?,
        val action: CouchEditTest.Action,
        val method: KFunction<*>,
        val inputDiffCollectionSize: Int,
        val durationMs: Double,
        val additionalProperties: Map<String, Any>
)
