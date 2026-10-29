package org.itineradev.itinera.policy

import org.itineradev.itinera.step.Reason

/**
 * A step policy written as a class, marked [StepPolicy]: hooks that act on the steps it is attached
 * to with `@WithStep(policies = ...)`. Each hook is a public method marked with its hook's
 * annotation, such as [OnStepSuccess]: an ordinary method in a synchronous workflow, and a
 * `suspend` one in an asynchronous workflow. The executor builds the class anew for every attempt,
 * with its constructor that takes nothing, so nothing a hook keeps outlives the attempt.
 *
 * The interface's methods make what the hooks return.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
 */
public interface ItineraStepPolicy {
    /**
     * Keeps the default, from any step hook.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
     */
    public fun keepDefault(): KeepDefault = KeepDefault

    /**
     * Finishes the workflow, from `on step success`: the journey succeeds at once.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
     */
    public fun finishWorkflow(): Lifecycle.FinishWorkflow = Lifecycle.FinishWorkflow

    /**
     * Fails the workflow for [reason], from any step hook: the journey fails at once.
     *
     * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
     */
    public fun failWorkflow(reason: Reason): Lifecycle.FailWorkflow = Lifecycle.FailWorkflow(reason)

    /**
     * Fails the workflow for a reason with [code] alone, from any step hook.
     *
     * @throws IllegalArgumentException when [code] is empty.
     * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
     */
    public fun failWorkflow(code: String): Lifecycle.FailWorkflow =
        Lifecycle.FailWorkflow(Reason(code))
}
