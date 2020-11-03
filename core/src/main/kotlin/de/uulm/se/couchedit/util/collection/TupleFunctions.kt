package de.uulm.se.couchedit.util.collection

fun <X: Comparable<X>> sortedPairOf(first: X, second: X): Pair<X, X> {
    return if(second < first) Pair(second, first) else Pair(first, second)
}

fun Pair<*, *>.joinToString(separator: CharSequence, prefix: CharSequence = "", postfix: CharSequence = ""): String {
    return prefix.toString() + first.toString() + separator + second.toString() + postfix
}
