package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookAction
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.HookScript
import org.itineradev.itinera.conformance.model.ValueType
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.HookKind
import org.itineradev.itinera.request.HookReporterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Mode

/**
 * The [hook] of [policy] as its [script] says, in mode [M], whose requests are of kind [K]: what it
 * requests, in the order written, then its contributor and its hook reporter, through [reporter],
 * and what it does before it ends.
 */
internal class ScriptedHook<M : Mode, K : HookKind<M>, R>(
    private val policy: String,
    private val hook: Hook,
    private val script: HookScript,
    private val reporter: Request<HookReporterKind<M>, R>,
) {
    private val requests =
        script.requests.associateWith { request -> ofKind<K>(requestOf(request)) }
    private val contributor = Requests.contributor()

    /** Everything the hook requests. */
    val declared: List<Request<K, *>>
        get() = requests.values + contributor + reporter

    /** What the hook does before it ends, in the order written. */
    val actions: List<HookAction>
        get() = script.actions

    /** What [got] gives the hook for one call, which [witness] records. */
    fun receivedFrom(got: Got<K>, witness: Witness): HookReceived<R> {
        val requested = requests.mapValues { entry -> got[entry.value] }
        witness.hookCalled(HookCall(policy, hook, requested))
        return HookReceived(requested, got[contributor], got[reporter])
    }
}

/** [request], for a hook whose requests are of kind [K], which the case trusts it may make. */
@Suppress("UNCHECKED_CAST")
private fun <K> ofKind(request: Request<*, *>): Request<K, *> = request as Request<K, *>

private fun requestOf(request: HookRequest): Request<*, *> =
    when (request) {
        is HookRequest.StepData ->
            if (request.optional) Requests.optionalFromStep(request.key, tokenOf(request.type))
            else Requests.fromStep(request.key, tokenOf(request.type))
        is HookRequest.WorkflowData ->
            if (request.optional) Requests.optionalFromWorkflow(request.key, tokenOf(request.type))
            else Requests.fromWorkflow(request.key, tokenOf(request.type))
        is HookRequest.FailureReason ->
            if (request.optional) Requests.optionalReason() else Requests.reason()
        HookRequest.Error -> Requests.error()
        HookRequest.FailureCause -> Requests.failureCause()
        HookRequest.RetryCause -> Requests.retryCause()
        HookRequest.StepName -> Requests.stepName()
        HookRequest.Attempt -> Requests.attempt()
        HookRequest.JourneyId -> Requests.journeyId()
        is HookRequest.Role -> Requests.role(roleOf(request.role))
    }

private fun tokenOf(type: ValueType): TypeToken<*> = TypeToken.of(type.type)
