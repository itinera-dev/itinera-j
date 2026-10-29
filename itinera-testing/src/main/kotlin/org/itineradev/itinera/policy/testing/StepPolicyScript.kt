package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.step.testing.ScriptedError

/**
 * How each hook of a [StepPolicyMock] ends, every one keeping the default unless the script says
 * otherwise, and whether building the mock fails: the factory of a step policy, which itinera calls
 * for every attempt, as it builds any step policy. A script never changes; each of its methods
 * gives a new one.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
 */
@ExperimentalItineraApi
public class StepPolicyScript
private constructor(
    internal val onStepSuccess: HookEnding,
    internal val onStepFailure: FailureHookEnding,
    internal val onStepRetry: FailureHookEnding,
    internal val onStepAbnormalTermination: FailureHookEnding,
    private val buildFailure: String?,
) : PolicyFactory<StepPolicyMock> {
    /**
     * The script, with every call of `on step success` ending as [ending] says.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepSuccess(ending: HookEnding): StepPolicyScript =
        StepPolicyScript(
            ending,
            onStepFailure,
            onStepRetry,
            onStepAbnormalTermination,
            buildFailure,
        )

    /**
     * The script, with every call of `on step failure` ending as [ending] says.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepFailure(ending: FailureHookEnding): StepPolicyScript =
        StepPolicyScript(
            onStepSuccess,
            ending,
            onStepRetry,
            onStepAbnormalTermination,
            buildFailure,
        )

    /**
     * The script, with every call of `on step retry` ending as [ending] says.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepRetry(ending: FailureHookEnding): StepPolicyScript =
        StepPolicyScript(
            onStepSuccess,
            onStepFailure,
            ending,
            onStepAbnormalTermination,
            buildFailure,
        )

    /**
     * The script, with every call of `on step abnormal termination` ending as [ending] says.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    public fun onStepAbnormalTermination(ending: FailureHookEnding): StepPolicyScript =
        StepPolicyScript(onStepSuccess, onStepFailure, onStepRetry, ending, buildFailure)

    /**
     * The script, with every build of the mock failing, throwing an exception carrying
     * [errorMessage], as a policy whose constructor fails does.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.failingToBuild
     */
    public fun failingToBuild(errorMessage: String): StepPolicyScript =
        StepPolicyScript(
            onStepSuccess,
            onStepFailure,
            onStepRetry,
            onStepAbnormalTermination,
            errorMessage,
        )

    /**
     * Builds a new mock, whose hooks end as the script says.
     *
     * @throws RuntimeException carrying the message of the script's build failure, when it has one.
     * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
     */
    override fun build(): StepPolicyMock {
        if (buildFailure != null) throw ScriptedError(buildFailure)
        return StepPolicyMock(this)
    }

    /** Makes step policy scripts. */
    public companion object {
        /**
         * The script whose every hook keeps the default, and whose mock is built.
         *
         * @sample org.itineradev.itinera.policy.testing.samples.stepPolicies
         */
        @JvmStatic
        public fun keepingDefault(): StepPolicyScript =
            StepPolicyScript(
                HookEnding.KeepDefault,
                HookEnding.KeepDefault,
                HookEnding.KeepDefault,
                HookEnding.KeepDefault,
                null,
            )
    }
}
