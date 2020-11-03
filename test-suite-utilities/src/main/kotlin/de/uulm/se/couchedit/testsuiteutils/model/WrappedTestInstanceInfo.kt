package de.uulm.se.couchedit.testsuiteutils.model

import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest

/**
 *
 */
internal data class WrappedTestInstanceInfo(
        val clazz: Class<out CouchEditTest>,
        val testInstanceInfo: TestInstanceInfo
)
