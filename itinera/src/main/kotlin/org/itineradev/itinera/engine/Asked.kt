package org.itineradev.itinera.engine

/**
 * What a request asks for, which the executor resolves before the part that declares it is built or
 * called.
 *
 * Part of itinera's engine, not of its API: `itinera-kotlin` and `itinera-java` make requests of
 * their own through [requestOf].
 */
public sealed interface Asked

/** Data from the workflow that a step requests under [key], read as [type]. */
internal data class InputAsked(
    /** The key. */
    val key: String,
    /** The type the value is read as. */
    val type: ValueType,
    /** Whether a missing value aborts the journey, rather than being answered absent. */
    val required: Boolean,
) : Asked {
    override fun toString(): String = (if (required) "" else "optional ") + "input $key: $type"
}

/** The service a step requests under [key], of [type]. */
internal data class ServiceAsked(
    /** The key. */
    val key: String,
    /** The service's type. */
    val type: Class<*>,
) : Asked {
    override fun toString(): String = "service $key: ${type.simpleName}"
}

/** A handle the executor makes for the call it is given to. */
public enum class Handle : Asked {
    /** The contributor, for the data the part contributes. */
    CONTRIBUTOR,

    /** A synchronous step's reporter, for its own events. */
    STEP_REPORTER,

    /** An asynchronous step's reporter, for its own events. */
    ASYNC_STEP_REPORTER;

    override fun toString(): String = name.lowercase().replace('_', ' ')
}

/**
 * How a request's answer is given to the part that made it, from what the executor resolved:
 * [ITSELF] for most, and `Optional` for those `itinera-java` makes.
 *
 * Part of itinera's engine, not of its API.
 */
public fun interface Answer {
    /**
     * The answer to give, from [resolved], which is `null` for an optional request answered absent.
     */
    public fun of(resolved: Any?): Any?

    /** Gives an answer. */
    public companion object {
        /** Gives what was resolved, as it is. */
        public val ITSELF: Answer = Answer { resolved -> resolved }
    }
}
