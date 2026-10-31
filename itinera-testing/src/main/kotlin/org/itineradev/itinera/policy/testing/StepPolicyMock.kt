package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess

/**
 * A step policy for tests, which a [StepPolicyScript] builds for each attempt: a method for each
 * step hook, which ends the call as its script says, through a [HookEnding], so that it ends
 * exactly as a real hook would. A test's hooks call the method of their hook last, after doing
 * whatever else they do, and return what it returns. Its methods suspend nothing, so the hooks of
 * either mode call them.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
 */
@ExperimentalItineraApi
public class StepPolicyMock internal constructor(private val script: StepPolicyScript) {
    /**
     * Ends a call of `on step success` as the script says.
     *
     * @throws RuntimeException carrying the message, when the script says the hook fails.
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepSuccess(): OnSuccess = script.onStepSuccess.onSuccess()

    /**
     * Ends a call of `on step failure` as the script says.
     *
     * @throws RuntimeException carrying the message, when the script says the hook fails.
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepFailure(): OnFailure = script.onStepFailure.onFailure()

    /**
     * Ends a call of `on step retry` as the script says.
     *
     * @throws RuntimeException carrying the message, when the script says the hook fails.
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepRetry(): OnFailure = script.onStepRetry.onFailure()

    /**
     * Ends a call of `on step abnormal termination` as the script says.
     *
     * @throws RuntimeException carrying the message, when the script says the hook fails.
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepAbnormalTermination(): OnFailure = script.onStepAbnormalTermination.onFailure()
}
