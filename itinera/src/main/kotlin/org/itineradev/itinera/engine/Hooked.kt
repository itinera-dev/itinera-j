package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStepHook
import org.itineradev.itinera.event.ByWorkflowHook
import org.itineradev.itinera.event.HookSource
import org.itineradev.itinera.event.StepHookSource
import org.itineradev.itinera.event.ValueSource
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName

/**
 * One call of a hook: who makes its requests, who gives the values it contributes, emits and sets,
 * what its events and contributions name as their [source], the facts it may request, and, for a
 * step hook, what it is [acting] on. It reads as the hook its events name, such as `hook on step
 * success of policy audit`.
 */
internal class Hooked<S : HookSource>
private constructor(
    val by: RequestedBy,
    val giver: ValueSource,
    val source: S,
    val known: Map<Known, Any>,
    val acting: ActingOn?,
    private val name: String,
) {
    /** The step it acts on, if it is a step hook. */
    val step: StepName?
        get() = by.step

    override fun toString(): String = name

    /** Makes the calls of hooks. */
    companion object {
        /**
         * A call of the step hook [hook] of [policy] acting on [attempt] of [step] in the journey
         * [journeyId], with what the attempt [left], which holds why it is called for `on step
         * failure` and `on step retry`.
         */
        fun ofStep(
            policy: PolicyName,
            hook: StepHook,
            step: StepName,
            attempt: Attempt,
            journeyId: JourneyId,
            left: Left,
        ): Hooked<StepHookSource> {
            val source = StepHookSource(policy, hook, step, attempt)
            val by = ByStepHook(policy, hook)
            val known =
                mapOf(
                    Known.JOURNEY_ID to journeyId,
                    Known.STEP_NAME to step,
                    Known.ATTEMPT to attempt,
                )
            return Hooked(
                RequestedBy(step, by, source),
                by,
                source,
                known + left.causes,
                ActingOn(left, step, by),
                "hook $hook of policy $policy",
            )
        }

        /** A call of the workflow hook [hook] of [policy] in the journey [journeyId]. */
        fun ofWorkflow(
            policy: PolicyName,
            hook: WorkflowHook,
            journeyId: JourneyId,
        ): Hooked<ByWorkflowHook> {
            val by = ByWorkflowHook(policy, hook)
            return Hooked(
                RequestedBy(null, by, by),
                by,
                by,
                mapOf(Known.JOURNEY_ID to journeyId),
                null,
                "hook $hook of policy $policy",
            )
        }
    }
}

/**
 * What an attempt left for the step hooks called after it: what it [contributed], committed or not,
 * which is nothing after an abnormal termination; the [reason] it reported, when it failed; the
 * [error] that ended it, when it ended in an abnormal termination; and, for `on step failure` and
 * `on step retry`, why the hook is called, among the facts in [causes].
 */
internal class Left(
    val contributed: Map<String, Captured>,
    val reason: Reason?,
    val error: Throwable?,
    val causes: Map<Known, Any> = emptyMap(),
) {
    /** The same, for `on step retry`, called for [cause]. */
    fun calledFor(cause: RetryCause): Left =
        Left(contributed, reason, error, mapOf(Known.RETRY_CAUSE to cause))

    /** The same, for `on step failure`, called for [cause]. */
    fun calledFor(cause: FailureCause): Left =
        Left(contributed, reason, error, mapOf(Known.FAILURE_CAUSE to cause))
}
