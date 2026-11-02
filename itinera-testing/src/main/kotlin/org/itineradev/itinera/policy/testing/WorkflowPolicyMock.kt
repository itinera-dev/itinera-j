package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * A workflow policy for tests, which a [WorkflowPolicyScript] builds for each journey: a method for
 * each workflow hook, which ends the call as its script says, through a [WorkflowHookEnding]. A
 * test's hooks call the method of their hook last, after doing whatever else they do. Its methods
 * suspend nothing, so the hooks of either mode call them.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
 */
@ExperimentalItineraApi
public class WorkflowPolicyMock internal constructor(private val script: WorkflowPolicyScript) {
    /**
     * Ends a call of `on workflow success` as the script says.
     *
     * @throws RuntimeException carrying the message, when the script says the hook fails.
     * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
     */
    public fun onWorkflowSuccess() {
        script.onWorkflowSuccess.played()
    }

    /**
     * Ends a call of `on workflow failure` as the script says.
     *
     * @throws RuntimeException carrying the message, when the script says the hook fails.
     * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
     */
    public fun onWorkflowFailure() {
        script.onWorkflowFailure.played()
    }
}
