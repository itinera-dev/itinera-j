package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.step.testing.ScriptedError

/**
 * How each hook of a [WorkflowPolicyMock] ends, every one returning unless the script says it
 * fails, and whether building the mock fails: the factory of a workflow policy, which itinera calls
 * once for every journey, as it builds any workflow policy. A script never changes; each of its
 * methods gives a new one.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
 */
@ExperimentalItineraApi
public class WorkflowPolicyScript
private constructor(
    internal val onWorkflowSuccess: WorkflowHookEnding,
    internal val onWorkflowFailure: WorkflowHookEnding,
    private val buildFailure: String?,
) : PolicyFactory<WorkflowPolicyMock> {
    /**
     * The script, with every call of `on workflow success` ending as [ending] says.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
     */
    public fun onWorkflowSuccess(ending: WorkflowHookEnding): WorkflowPolicyScript =
        WorkflowPolicyScript(ending, onWorkflowFailure, buildFailure)

    /**
     * The script, with every call of `on workflow failure` ending as [ending] says.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
     */
    public fun onWorkflowFailure(ending: WorkflowHookEnding): WorkflowPolicyScript =
        WorkflowPolicyScript(onWorkflowSuccess, ending, buildFailure)

    /**
     * The script, with every build of the mock failing, throwing an exception carrying
     * [errorMessage], as a policy whose constructor fails does.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.failingToBuild
     */
    public fun failingToBuild(errorMessage: String): WorkflowPolicyScript =
        WorkflowPolicyScript(onWorkflowSuccess, onWorkflowFailure, errorMessage)

    /**
     * Builds a new mock, whose hooks end as the script says.
     *
     * @throws RuntimeException carrying the message of the script's build failure, when it has one.
     * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
     */
    override fun build(): WorkflowPolicyMock {
        if (buildFailure != null) throw ScriptedError(buildFailure)
        return WorkflowPolicyMock(this)
    }

    /** Makes workflow policy scripts. */
    public companion object {
        /**
         * The script whose every hook returns, and whose mock is built.
         *
         * @sample org.itineradev.itinera.policy.testing.samples.workflowPolicies
         */
        @JvmStatic
        public fun returning(): WorkflowPolicyScript =
            WorkflowPolicyScript(HookEnding.KeepDefault, HookEnding.KeepDefault, null)
    }
}
