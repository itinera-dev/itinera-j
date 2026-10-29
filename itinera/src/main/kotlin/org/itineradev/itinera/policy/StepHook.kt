package org.itineradev.itinera.policy

/**
 * The hooks of a step policy, each written as the specification names it, such as `on step
 * success`.
 *
 * @sample org.itineradev.itinera.policy.samples.hooks
 */
public enum class StepHook(private val text: String) {
    /** Called after an attempt reported Success. */
    ON_STEP_SUCCESS("on step success"),
    /** Called once a step is given up and ends as failed. */
    ON_STEP_FAILURE("on step failure"),
    /** Called before a step is attempted again. */
    ON_STEP_RETRY("on step retry"),
    /** Called after an attempt ended in an abnormal termination. */
    ON_STEP_ABNORMAL_TERMINATION("on step abnormal termination");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
