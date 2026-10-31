package org.itineradev.itinera.conformance

/**
 * One of the Kotlin runner's two columns: every scenario runs once as synchronous Kotlin on
 * `LocalExecutor` and once as asynchronous Kotlin on `AsyncLocalExecutor`, except a scenario that
 * needs one mode, which runs only in that mode's column.
 */
internal enum class Column(
    /** The tag the report gives each scenario of this column, so the two runs stay apart. */
    val tag: String,
    /** The capability whose scenarios this column leaves to the other. */
    private val otherMode: String,
    /** The package of the glue that tells a scenario which column it runs in. */
    val glue: String,
) {
    SYNCHRONOUS(
        "@executor-local",
        "@capability-async",
        "org.itineradev.itinera.conformance.columns.local",
    ),
    ASYNCHRONOUS(
        "@executor-async",
        "@capability-sync",
        "org.itineradev.itinera.conformance.columns.async",
    );

    /** The scenarios of this column among those the tag expression selects. */
    fun tags(selected: String): String = "($selected) and not $otherMode"
}
