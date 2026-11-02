package org.itineradev.itinera.engine

/**
 * What a reporter handle emits through and sets global metadata through, in either mode: the engine
 * makes one for each attempt, call or hook, and closes it when that ends. Each call returns once
 * every reporter has what it gave.
 *
 * Part of itinera's engine, not of its API.
 */
public interface Emits {
    /**
     * Emits the event of [severity] with [message] and [data], `null` when none was given.
     *
     * @throws IllegalStateException once it is closed.
     */
    @JvmSynthetic public suspend fun emit(severity: Severity, message: String, data: Any?)

    /**
     * Sets global metadata [key] to [value] on every reporter.
     *
     * @throws IllegalStateException once it is closed.
     */
    @JvmSynthetic public suspend fun setGlobalMetadata(key: String, value: Any?)

    /** Closes it, once its attempt, call or hook has ended. */
    public fun close()
}

/**
 * A reporter handle of one mode, made for one attempt, call or hook: [reporter], given to custom
 * code that requests [handle]. It lets one call through to its [Emits] at a time, whichever thread
 * or coroutine makes it, so events keep the order of the calls.
 *
 * Part of itinera's engine, not of its API.
 */
public interface ReporterHandle {
    /** What custom code requests it as. */
    public val handle: Handle

    /** The reporter custom code is given. */
    public val reporter: Any

    /** Closes its [Emits] once no call is going through, so no call goes through afterwards. */
    @JvmSynthetic public suspend fun close()
}

/**
 * Makes the reporter handles of one mode, each reporting through the [Emits] it is given.
 *
 * Part of itinera's engine, not of its API.
 */
public interface ReporterHandles {
    /** The step reporter for one attempt, which reports through [emits]. */
    public fun stepReporter(emits: Emits): ReporterHandle

    /** The hook reporter for one call of a hook, which reports through [emits]. */
    public fun hookReporter(emits: Emits): ReporterHandle
}

/**
 * How serious an emitted event is: information, a warning or an error.
 *
 * Part of itinera's engine, not of its API.
 */
public enum class Severity {
    /** Information. */
    INFO,

    /** A warning. */
    WARNING,

    /** An error. */
    ERROR,
}
