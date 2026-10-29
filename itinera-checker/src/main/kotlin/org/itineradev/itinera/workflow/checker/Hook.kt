package org.itineradev.itinera.workflow.checker

/**
 * The annotation that marks a method of a policy class as one hook: its binary name, the hook's
 * name, whether a step policy or a workflow policy defines it, and what it returns, or `null` for
 * nothing.
 */
internal enum class Hook(
    val annotation: String,
    val hook: String,
    val ofStep: Boolean,
    val returns: String?,
) {
    ON_STEP_SUCCESS("OnStepSuccess", "on step success", Names.ON_SUCCESS),
    ON_STEP_FAILURE("OnStepFailure", "on step failure", Names.ON_FAILURE),
    ON_STEP_RETRY("OnStepRetry", "on step retry", Names.ON_FAILURE),
    ON_STEP_ABNORMAL_TERMINATION(
        "OnStepAbnormalTermination",
        "on step abnormal termination",
        Names.ON_FAILURE,
    ),
    ON_WORKFLOW_SUCCESS("OnWorkflowSuccess", "on workflow success", null),
    ON_WORKFLOW_FAILURE("OnWorkflowFailure", "on workflow failure", null);

    constructor(
        simpleName: String,
        hook: String,
        returns: String?,
    ) : this("org.itineradev.itinera.policy.$simpleName", hook, returns != null, returns)

    /** The annotation as it is written, such as `@OnStepSuccess`. */
    val written: String
        get() = "@" + annotation.substringAfterLast('.')
}
