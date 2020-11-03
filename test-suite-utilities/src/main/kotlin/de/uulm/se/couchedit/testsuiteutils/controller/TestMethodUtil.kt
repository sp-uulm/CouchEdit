package de.uulm.se.couchedit.testsuiteutils.controller

import org.junit.jupiter.api.Order
import kotlin.reflect.KFunction

object TestMethodUtil {
    fun getMethodOrderNumber(method: KFunction<*>): Int? {
        val order = method.annotations.filterIsInstance<Order>().firstOrNull()

        return order?.value
    }
}
