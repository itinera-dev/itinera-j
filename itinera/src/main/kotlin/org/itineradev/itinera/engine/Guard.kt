package org.itineradev.itinera.engine

import org.itineradev.itinera.event.EventKind

/**
 * The first failure of any reporter of a journey, once one failed.
 *
 * Part of itinera's engine, not of its API.
 */
public class Failures internal constructor() {
    /** What the first reporter that failed threw, if one failed. */
    public var first: Throwable? = null
        private set

    /** Keeps what a reporter threw, unless one failed before. */
    public fun record(thrown: Throwable) {
        if (first == null) first = thrown
    }
}

/**
 * Stands between the dispatcher and one reporter of a journey, so the rules on a reporter that
 * fails hold whatever the dispatcher does. Once any reporter failed, it lets nothing through but
 * `journey_aborted`, and nothing at all to its own reporter if that one failed. What its reporter
 * throws on `journey_aborted` is ignored, so delivery goes on to the reporters after it; an [Error]
 * is never caught.
 *
 * Part of itinera's engine, not of its API.
 */
public class Guard internal constructor(@PublishedApi internal val failures: Failures) {
    @PublishedApi internal var failed: Boolean = false

    /** Delivers an event of [kind] to its reporter, through [deliver], when the rules allow. */
    public inline fun report(kind: EventKind, deliver: () -> Unit) {
        if (kind == EventKind.JOURNEY_ABORTED) {
            if (!failed) runCatching(deliver).onFailure(::rethrowError)
        } else if (failures.first == null) recording(deliver)
    }

    /** Sets global metadata on its reporter, through [deliver], when the rules allow. */
    public inline fun setGlobalMetadata(deliver: () -> Unit) {
        if (failures.first == null) recording(deliver)
    }

    @PublishedApi
    internal inline fun recording(deliver: () -> Unit) {
        runCatching(deliver).onFailure(::rethrowError).onFailure(::fail).getOrThrow()
    }

    @PublishedApi
    internal fun fail(thrown: Throwable) {
        failed = true
        failures.record(thrown)
    }
}
