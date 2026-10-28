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

/** Data a step contributed that a hook requests under [key], read as [type]. */
internal data class FromStepAsked(
    /** The key. */
    val key: String,
    /** The type the value is read as. */
    val type: ValueType,
    /** Whether a missing value aborts the journey, rather than being answered absent. */
    val required: Boolean,
) : Asked {
    override fun toString(): String =
        (if (required) "" else "optional ") + "data from the step $key: $type"
}

/** Data from the workflow that a hook requests under [key], read as [type] from the data bag. */
internal data class FromWorkflowAsked(
    /** The key. */
    val key: String,
    /** The type the value is read as. */
    val type: ValueType,
    /** Whether a missing value aborts the journey, rather than being answered absent. */
    val required: Boolean,
) : Asked {
    override fun toString(): String =
        (if (required) "" else "optional ") + "data from the workflow $key: $type"
}

/** The reason a step's attempt reported for failing. */
internal data class ReasonAsked(
    /** Whether a missing reason aborts the journey, rather than being answered absent. */
    val required: Boolean
) : Asked {
    override fun toString(): String = (if (required) "" else "optional ") + "reason"
}

/** The error that ended a step's attempt abnormally. */
internal data class ErrorAsked(
    /** Whether a missing error aborts the journey, rather than being answered absent. */
    val required: Boolean
) : Asked {
    override fun toString(): String = (if (required) "" else "optional ") + "error"
}

/** The role of [type] that the workflow provides. */
internal data class RoleAsked(
    /** The role's interface. */
    val type: Class<*>
) : Asked {
    override fun toString(): String = "role ${type.simpleName}"
}

/** What the executor knows about the call it is given to. */
internal enum class Known(private val text: String) : Asked {
    /** The journey's ID. */
    JOURNEY_ID("journey ID"),

    /** The name of the step the part acts on. */
    STEP_NAME("step name"),

    /** The key of the input an input adapter is asked for. */
    KEY("key"),

    /** The attempt the part acts on. */
    ATTEMPT("attempt"),

    /** Why the step failed. */
    FAILURE_CAUSE("failure cause"),

    /** Why the step is retried. */
    RETRY_CAUSE("retry cause");

    override fun toString(): String = text
}

/** A handle the executor makes for the call it is given to. */
public enum class Handle : Asked {
    /** The contributor, for the data the part contributes. */
    CONTRIBUTOR,

    /** A synchronous step's reporter, for its own events. */
    STEP_REPORTER,

    /** An asynchronous step's reporter, for its own events. */
    ASYNC_STEP_REPORTER,

    /** A synchronous hook's reporter, for its own events. */
    HOOK_REPORTER,

    /** An asynchronous hook's reporter, for its own events. */
    ASYNC_HOOK_REPORTER,

    /** An input adapter's read access to the data bag. */
    DATA_BAG_ACCESS,

    /** A synchronous service provider's reporter, for its own events. */
    PROVIDER_REPORTER,

    /** An asynchronous service provider's reporter, for its own events. */
    ASYNC_PROVIDER_REPORTER;

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
