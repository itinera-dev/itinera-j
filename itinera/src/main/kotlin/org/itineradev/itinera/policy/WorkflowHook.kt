package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * The hooks of a workflow policy, called once the journey's outcome is known, each written as the
 * specification names it, such as `on workflow success`.
 *
 * @sample org.itineradev.itinera.policy.samples.hooks
 */
@ExperimentalItineraApi
public enum class WorkflowHook(private val text: String) {
    /** Called once the journey succeeded. */
    ON_WORKFLOW_SUCCESS("on workflow success"),
    /** Called once the journey failed. */
    ON_WORKFLOW_FAILURE("on workflow failure");

    /** The name, as the specification writes it. */
    override fun toString(): String = text
}
