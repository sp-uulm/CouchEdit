package de.uulm.se.couchedit.debugui.util

object StringUtil {
    /**
     * Returns the "CamelHumps" of an input String [str].
     * I.e. only uppercase letters are preserved, and only if the previous letter was lowercase.
     */
    fun toCamelHumps(str: String): String {
        return str.filterIndexed { index, c ->
            c.isLowerCase() || index == 0 || str[index-1].isLowerCase()
        }.filter { it.isUpperCase() }
    }
}
