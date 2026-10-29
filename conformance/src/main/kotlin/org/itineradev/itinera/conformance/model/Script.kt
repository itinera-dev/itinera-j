package org.itineradev.itinera.conformance.model

/** How a scripted attempt ends, as the script's `ending` column writes it. */
internal enum class ScriptedEnding(val written: String) {
    SUCCESS("success"),
    FAILURE("failure"),
    SKIPPED("skipped"),
    ABNORMAL_TERMINATION("abnormal termination"),
    REAL_RUN("real run");

    companion object {
        private val byName = entries.associateBy(ScriptedEnding::written)

        /** The ending a script names. */
        fun named(written: String): ScriptedEnding =
            byName[written] ?: throw ModelException("$written is not an ending of a script")
    }
}

/**
 * One row of the script: the [step] it names, the attempts it chooses, as [attempt] writes them,
 * `null` when the first row of a step leaves attempt 1 unsaid, and what they do: their
 * contributions and entries of global metadata, each in the order written, the event they emit with
 * its message, and how they end, with the reason's [code] or the error's [message].
 */
internal data class ScriptRow(
    val step: String,
    val attempt: String?,
    val contributes: Map<String, Any?>,
    val emits: Emitted?,
    val emitMessage: String?,
    val setsGlobalMetadata: Map<String, Any?>,
    val ending: ScriptedEnding,
    val code: String?,
    val message: String?,
)
