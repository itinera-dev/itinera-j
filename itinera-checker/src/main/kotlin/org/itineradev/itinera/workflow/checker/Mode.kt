package org.itineradev.itinera.workflow.checker

/**
 * The mode of a workflow class, from the workflow interface it implements, with the handles a part
 * of that mode requests by their types and why a part cannot request each handle of the other mode.
 */
internal enum class Mode(
    val workflow: String,
    val reporter: String,
    val stepReporter: String,
    val hookReporter: String,
    val providerReporter: String,
    val written: String,
    private val withArticle: String,
) {
    SYNC(
        Names.ITINERA_WORKFLOW,
        Names.REPORTER,
        Names.STEP_REPORTER,
        Names.HOOK_REPORTER,
        Names.PROVIDER_REPORTER,
        "synchronous",
        "a synchronous",
    ),
    ASYNC(
        Names.ITINERA_ASYNC_WORKFLOW,
        Names.ASYNC_REPORTER,
        Names.ASYNC_STEP_REPORTER,
        Names.ASYNC_HOOK_REPORTER,
        Names.ASYNC_PROVIDER_REPORTER,
        "asynchronous",
        "an asynchronous",
    );

    /** Whether a part of this mode is a `suspend` function. */
    val suspends: Boolean
        get() = this == ASYNC

    /** The other mode. */
    val other: Mode
        get() = if (this == SYNC) ASYNC else SYNC

    /** Why a [part] of this mode, such as a step, cannot request [handle] of the other mode. */
    fun otherMode(handle: String, part: String): String =
        "requests ${simpleNameOf(handle)}, which only ${other.withArticle} $part can"
}

/** The simple name of the class whose binary name is [name], when it is a top-level class. */
internal fun simpleNameOf(name: String): String = name.substringAfterLast('.')
