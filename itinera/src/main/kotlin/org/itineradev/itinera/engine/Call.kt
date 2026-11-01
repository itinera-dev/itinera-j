package org.itineradev.itinera.engine

import org.itineradev.itinera.request.Request

/**
 * What the executor gives one call of custom code, which its requests may ask for: who makes the
 * call's requests, the facts it [knows][known], and the [handles] made for it.
 */
internal class Call(
    val by: RequestedBy,
    private val known: Map<Known, Any>,
    private val handles: Map<Handle, Any>,
) {
    /**
     * Whether [resolved] answers [request] for this call: inputs and services are resolved while a
     * step is built, and what this version of itinera does not resolve yet is left unanswered.
     */
    fun answers(request: Request<*, *>): Boolean = isAnswered(request.asked)

    /** The fact [asked] names. */
    fun known(asked: Known): Any = known[asked] ?: throw AssertionError("Nothing knows the $asked.")

    /** The handle [asked] names. */
    fun handle(asked: Handle): Any = handles[asked] ?: throw AssertionError("No $asked is made.")

    private fun isAnswered(asked: Asked): Boolean =
        when (asked) {
            is FromWorkflowAsked,
            is Known -> true
            is Handle -> asked in handles
            is InputAsked,
            is ServiceAsked,
            is FromStepAsked,
            is ReasonAsked,
            is ErrorAsked,
            is RoleAsked -> false
        }
}

/**
 * Resolves each of [requests] that [call] answers, in order: data from the workflow from the data
 * bag, and facts and handles from [call].
 *
 * @throws JourneyAbortSignal when resolving a request aborts the journey.
 */
@JvmSynthetic
internal suspend fun Journey.resolved(
    requests: List<Request<*, *>>,
    call: Call,
): Map<Request<*, *>, Any?> =
    requests.filter(call::answers).associateWith { request -> answer(request.asked, call) }

private suspend fun Journey.answer(asked: Asked, call: Call): Any? =
    when (asked) {
        is FromWorkflowAsked -> fromWorkflow(asked.key, asked.type, asked.required, call.by)
        is Known -> call.known(asked)
        is Handle -> call.handle(asked)
        is InputAsked,
        is ServiceAsked,
        is FromStepAsked,
        is ReasonAsked,
        is ErrorAsked,
        is RoleAsked -> throw AssertionError("A call of this kind cannot request $asked.")
    }
