package org.itineradev.itinera.conformance.trace

/** The data an event table says an event carries: unstated, none, or this value. */
internal sealed interface Data {
    /** The table does not say. */
    data object Unstated : Data

    /** The event carries no data. */
    data object None : Data

    /** The event carries [value]. */
    data class Is(val value: Any?) : Data
}

/**
 * One event as an event table describes it: its kind, the cells of its other columns, its data, and
 * what else it carries that no column shows, such as a reason's message and details.
 */
internal data class Line(
    val event: String,
    val cells: Map<String, String>,
    val data: Data,
    val carried: List<Any?>,
)

/** The columns an event table may have besides `event` and `data`. */
internal val columns =
    listOf(
        "step",
        "attempt",
        "key",
        "code",
        "policy",
        "hook",
        "lifecycle",
        "adapter",
        "provider",
        "implementation",
        "source",
        "cause",
        "rescue",
        "decided by",
        "message",
        "error",
    )
