package org.itineradev.itinera.policy.testing

import org.itineradev.itinera.policy.OnFailure
import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.testing.ScriptedError

/**
 * How a test says a call of a hook of a policy mock ends, so that the mock ends it exactly as a
 * real hook would: it keeps the default, which for a workflow hook means it returns, it finishes or
 * fails the workflow, or it fails, throwing an exception, as a hook that fails does. Only `on step
 * success` may finish the workflow, so the other step hooks take a [FailureHookEnding], and
 * workflow hooks, which decide nothing, a [WorkflowHookEnding].
 *
 * @sample org.itineradev.itinera.policy.testing.samples.endings
 */
public sealed interface HookEnding {
    /**
     * What `on step success` returns to end this way.
     *
     * @throws RuntimeException carrying the message, for [Fails], as a hook that fails throws it.
     * @sample org.itineradev.itinera.policy.testing.samples.onSuccess
     */
    public fun onSuccess(): OnSuccess =
        when (this) {
            KeepDefault -> OnSuccess.keepDefault()
            FinishWorkflow -> OnSuccess.finishWorkflow()
            is FailWorkflow -> OnSuccess.failWorkflow(reason)
            is Fails -> throw ScriptedError(errorMessage)
        }

    /**
     * The hook keeps the default: a step hook returns `keepDefault()`, and a workflow hook returns.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.endings
     */
    public data object KeepDefault : FailureHookEnding, WorkflowHookEnding

    /**
     * `on step success` returns `finishWorkflow()`.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.endings
     */
    public data object FinishWorkflow : HookEnding

    /**
     * A step hook returns `failWorkflow(reason)`.
     *
     * @sample org.itineradev.itinera.policy.testing.samples.endings
     */
    @JvmRecord
    public data class FailWorkflow(
        /** The reason the journey fails for. */
        val reason: Reason
    ) : FailureHookEnding

    /**
     * The hook fails, throwing an exception whose message is [errorMessage].
     *
     * @sample org.itineradev.itinera.policy.testing.samples.endings
     */
    @JvmRecord
    public data class Fails(
        /** The message of the exception the hook throws. */
        val errorMessage: String
    ) : FailureHookEnding, WorkflowHookEnding

    /** Makes hook endings. */
    public companion object {
        /**
         * The hook keeps the default, or a workflow hook returns.
         *
         * @sample org.itineradev.itinera.policy.testing.samples.endings
         */
        @JvmStatic public fun keepDefault(): KeepDefault = KeepDefault

        /**
         * `on step success` finishes the workflow.
         *
         * @sample org.itineradev.itinera.policy.testing.samples.endings
         */
        @JvmStatic public fun finishWorkflow(): HookEnding = FinishWorkflow

        /**
         * A step hook fails the workflow, for [reason].
         *
         * @sample org.itineradev.itinera.policy.testing.samples.endings
         */
        @JvmStatic public fun failWorkflow(reason: Reason): FailureHookEnding = FailWorkflow(reason)

        /**
         * A step hook fails the workflow, for a reason holding only [code].
         *
         * @sample org.itineradev.itinera.policy.testing.samples.endings
         */
        @JvmStatic
        public fun failWorkflow(code: String): FailureHookEnding = FailWorkflow(Reason(code))

        /**
         * The hook fails, throwing an exception carrying [errorMessage].
         *
         * @sample org.itineradev.itinera.policy.testing.samples.endings
         */
        @JvmStatic public fun fails(errorMessage: String): Fails = Fails(errorMessage)
    }
}

/**
 * How a call of a step hook that cannot finish the workflow ends: any step hook but `on step
 * success`.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.endings
 */
public sealed interface FailureHookEnding : HookEnding {
    /**
     * What the hook returns to end this way.
     *
     * @throws RuntimeException carrying the message, for [HookEnding.Fails], as a hook that fails
     *   throws it.
     * @sample org.itineradev.itinera.policy.testing.samples.onFailure
     */
    public fun onFailure(): OnFailure =
        when (this) {
            HookEnding.KeepDefault -> OnFailure.keepDefault()
            is HookEnding.FailWorkflow -> OnFailure.failWorkflow(reason)
            is HookEnding.Fails -> throw ScriptedError(errorMessage)
        }
}

/**
 * How a call of a workflow hook ends: it returns, or it fails.
 *
 * @sample org.itineradev.itinera.policy.testing.samples.endings
 */
public sealed interface WorkflowHookEnding {
    /**
     * Ends the call this way: returns, or throws.
     *
     * @throws RuntimeException carrying the message, for [HookEnding.Fails], as a hook that fails
     *   throws it.
     * @sample org.itineradev.itinera.policy.testing.samples.onWorkflow
     */
    public fun played() {
        when (this) {
            HookEnding.KeepDefault -> Unit
            is HookEnding.Fails -> throw ScriptedError(errorMessage)
        }
    }
}
