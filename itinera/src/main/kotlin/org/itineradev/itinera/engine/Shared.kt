package org.itineradev.itinera.engine

/** Each of [values] that occurs more than once among them, once. */
internal fun <T> shared(values: List<T>): Set<T> =
    values.groupingBy(::itself).eachCount().filterValues(::isShared).keys

private fun <T> itself(value: T): T = value

/** Whether a value counted [count] times is shared. */
internal fun isShared(count: Int): Boolean = count > 1
