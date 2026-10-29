package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStepHook
import org.itineradev.itinera.event.InvalidLifecycle
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.StepHookSource
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.policy.KeepDefault
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.policy.StepHook

/**
 * The lifecycle [returned], what the step hook [hooked] returned, once checked: `null` when it kept
 * the default, and otherwise a lifecycle it may return, with a captured copy of its reason's
 * details. Only `on step success` may finish the workflow. Kotlin's types and the sealed
 * `OnSuccess` and `OnFailure` rule out the rest, but a Java hook can return `null`, and an
 * unchecked cast can hide anything.
 *
 * @throws JourneyAbortSignal with `invalid lifecycle` when a failure hook finished the workflow,
 *   with `not a value` when the details of the reason it failed the workflow with are not a value,
 *   or with `unexpected result` when it returned anything else, `null` among it.
 */
internal fun Journey.lifecycleOf(hooked: Hooked<StepHookSource>, returned: Any?): Lifecycle? =
    when (returned) {
        KeepDefault -> null
        Lifecycle.FinishWorkflow -> finishing(hooked.source)
        is Lifecycle.FailWorkflow -> failing(hooked.source, returned)
        else -> throw unexpected(hooked, returned)
    }

private fun Journey.finishing(source: StepHookSource): Lifecycle =
    if (source.hook == StepHook.ON_STEP_SUCCESS) Lifecycle.FinishWorkflow
    else throw abort(Abort(InvalidLifecycle(source.step), null))

private fun Journey.failing(source: StepHookSource, failing: Lifecycle.FailWorkflow): Lifecycle {
    val reason = failing.reason
    return when (val capture = Values.capture(reason.details)) {
        is Captured -> Lifecycle.FailWorkflow(reason.copy(details = capture.value))
        is NotAValue -> {
            val by = ByStepHook(source.policy, source.hook)
            val abort = NotAValueAbort.InDetails(source.step, by, capture.error?.let(::messageOf))
            throw abort(Abort(abort, capture.error))
        }
    }
}

private fun Journey.unexpected(hooked: Hooked<StepHookSource>, returned: Any?): JourneyAbortSignal {
    val source = hooked.source
    val what = if (returned == null) "null" else "an object of ${returned.javaClass.name}"
    val message = "the $hooked returned $what, not keepDefault() or a lifecycle"
    val result = UnexpectedResult(source.step, ByStepHook(source.policy, source.hook), message)
    return abort(Abort(result, null))
}
