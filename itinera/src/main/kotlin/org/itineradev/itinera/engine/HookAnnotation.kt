package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnStepAbnormalTermination
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepRetry
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.OnWorkflowFailure
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook

/**
 * The annotation that marks a method of a policy class as one hook: the annotation, the hook's
 * name, whether a step policy or a workflow policy defines it, and what it returns, or `null` for
 * nothing.
 */
internal enum class HookAnnotation(
    val annotation: Class<out Annotation>,
    val hook: String,
    val ofStep: Boolean,
    val returns: Class<*>?,
) {
    ON_STEP_SUCCESS(OnStepSuccess::class.java, StepHook.ON_STEP_SUCCESS, OnSuccess::class.java),
    ON_STEP_FAILURE(OnStepFailure::class.java, StepHook.ON_STEP_FAILURE, OnFailure::class.java),
    ON_STEP_RETRY(OnStepRetry::class.java, StepHook.ON_STEP_RETRY, OnFailure::class.java),
    ON_STEP_ABNORMAL_TERMINATION(
        OnStepAbnormalTermination::class.java,
        StepHook.ON_STEP_ABNORMAL_TERMINATION,
        OnFailure::class.java,
    ),
    ON_WORKFLOW_SUCCESS(OnWorkflowSuccess::class.java, WorkflowHook.ON_WORKFLOW_SUCCESS),
    ON_WORKFLOW_FAILURE(OnWorkflowFailure::class.java, WorkflowHook.ON_WORKFLOW_FAILURE);

    constructor(
        annotation: Class<out Annotation>,
        hook: StepHook,
        returns: Class<*>,
    ) : this(annotation, hook.toString(), ofStep = true, returns)

    constructor(
        annotation: Class<out Annotation>,
        hook: WorkflowHook,
    ) : this(annotation, hook.toString(), ofStep = false, returns = null)

    /** The annotation as it is written, such as `@OnStepSuccess`. */
    val written: String
        get() = "@${annotation.simpleName}"
}
