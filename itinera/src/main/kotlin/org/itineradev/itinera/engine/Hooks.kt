package org.itineradev.itinera.engine

import org.itineradev.itinera.event.DecidingHook
import org.itineradev.itinera.event.HookFailed
import org.itineradev.itinera.event.StepHookCalled
import org.itineradev.itinera.event.WorkflowHookCalled
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode

/**
 * Calls the hooks of [instance]'s journey through [parts]. A hook's requests are resolved in the
 * order it declares them, then it is called with its own contributor and hook reporter, which close
 * once it has returned. Once it returned without failing, what it returned is checked,
 * `hook_called` reports it, and what it contributed is committed.
 */
internal class Hooks<M : Mode>(
    private val journey: Journey,
    private val parts: Parts<M>,
    private val instance: WorkflowInstance<M>,
) {
    /**
     * Calls [hook] of the policy among [policies] that defines it, if one does, acting on [attempt]
     * of [step], with what the attempt [left]: the lifecycle it returned, with the hook that
     * returned it, or `null` when no policy defines it or it kept the default.
     *
     * @throws JourneyAbortSignal when resolving a request aborts the journey, when the journey is
     *   aborted while the hook runs, whatever it does afterwards, with `hook failed` when it fails,
     *   and when what it returned is not what it may return.
     */
    @JvmSynthetic
    suspend fun ofStep(
        policies: List<BuiltPolicy<StepHook>>,
        hook: StepHook,
        step: StepName,
        attempt: Attempt,
        left: Left,
    ): Decided? {
        val defining =
            policies.firstNotNullOfOrNull { built -> built.defining(hook) } ?: return null
        val defined = defining.hook
        val hooked = Hooked.ofStep(defined.policy, hook, step, attempt, instance.journeyId, left)
        val returned =
            called(defined, hooked) { got ->
                parts.hooks.step(defined.definition.call, defining.policy, got)
            }
        val lifecycle = journey.lifecycleOf(hooked, returned.value)
        journey.emit {
            StepHookCalled(sequence, timestamp, journeyId, workflow, hooked.source, lifecycle)
        }
        journey.commit(returned.contributed, hooked.source)
        return lifecycle?.let { decided -> Decided(decided, DecidingHook(defined.policy, hook)) }
    }

    /**
     * Calls [hook] of the policy among [policies] that defines it, if one does.
     *
     * @throws JourneyAbortSignal when resolving a request aborts the journey, when the journey is
     *   aborted while the hook runs, whatever it does afterwards, and with `hook failed` when it
     *   fails.
     */
    @JvmSynthetic
    suspend fun ofWorkflow(policies: List<BuiltPolicy<WorkflowHook>>, hook: WorkflowHook) {
        val defining = policies.firstNotNullOfOrNull { built -> built.defining(hook) } ?: return
        val defined = defining.hook
        val hooked = Hooked.ofWorkflow(defined.policy, hook, instance.journeyId)
        val returned =
            called(defined, hooked) { got ->
                parts.hooks.workflow(defined.definition.call, defining.policy, got)
            }
        journey.emit { WorkflowHookCalled(sequence, timestamp, journeyId, workflow, hooked.source) }
        journey.commit(returned.contributed, hooked.source)
    }

    /**
     * Resolves what [defined] requests for [hooked], then makes the call, through [call], with its
     * own handles, closed once it has returned.
     */
    private suspend fun called(
        defined: DefinedHook<*>,
        hooked: Hooked<*>,
        call: suspend (Got<*>) -> Any?,
    ): Returned {
        val contributions = Contributions(journey, hooked.step, hooked.giver, "The $hooked")
        val reporter = parts.reporterHandles.hookReporter(HookEmits(journey, hooked))
        val handles =
            mapOf(Handle.CONTRIBUTOR to contributions, reporter.handle to reporter.reporter)
        val result =
            try {
                val got =
                    Answered<Any?>(defined.requests, answers(defined.requests, hooked, handles))
                parts.calling { call(got) }
            } finally {
                contributions.close()
                reporter.close()
            }
        journey.ensureNotAborted()
        val value = result.getOrElse { thrown -> throw failed(hooked, thrown) }
        return Returned(value, contributions.contributed)
    }

    private suspend fun answers(
        requests: List<Request<*, *>>,
        hooked: Hooked<*>,
        handles: Map<Handle, Any>,
    ): Map<Request<*, *>, Any?> = requests.associateWith { request ->
        answer(request.asked, hooked, handles)
    }

    private suspend fun answer(asked: Asked, hooked: Hooked<*>, handles: Map<Handle, Any>): Any? =
        when (asked) {
            is FromWorkflowAsked ->
                journey.fromWorkflow(asked.key, asked.type, asked.required, hooked.by)
            is FromStepAsked ->
                journey.fromStep(
                    actingOn(hooked, asked).left.contributed,
                    asked.key,
                    asked.type,
                    asked.required,
                    hooked.by,
                )
            is ReasonAsked -> journey.reasonFor(actingOn(hooked, asked), asked)
            is ErrorAsked -> journey.errorFor(actingOn(hooked, asked), asked)
            is RoleAsked -> journey.roleOf(asked.type, instance.workflow, hooked.step)
            is Known -> hooked.known[asked] ?: throw notAnswered(hooked, asked)
            is Handle -> handles[asked] ?: throw notAnswered(hooked, asked)
            is InputAsked,
            is ServiceAsked -> throw notAnswered(hooked, asked)
        }

    private fun actingOn(hooked: Hooked<*>, asked: Asked): ActingOn =
        hooked.acting ?: throw notAnswered(hooked, asked)

    /**
     * Aborts the journey with `hook failed` for a request of [hooked] that itinera does not answer
     * for its kind of hook, which only a descriptor that types do not check can make.
     */
    private fun notAnswered(hooked: Hooked<*>, asked: Asked): JourneyAbortSignal =
        failed(hooked, IllegalStateException("itinera gives the $hooked no $asked"))

    /** Aborts the journey with `hook failed`, carrying what [hooked] threw. */
    private fun failed(hooked: Hooked<*>, thrown: Throwable): JourneyAbortSignal =
        journey.abort(Abort(HookFailed(hooked.step, messageOf(thrown)), thrown))
}

/** A lifecycle a step hook returned, and the hook that returned it, which decides with it. */
internal class Decided(val lifecycle: Lifecycle, val by: DecidingHook)

/** What a hook returned, and what it contributed. */
private class Returned(val value: Any?, val contributed: Map<String, Captured>)
