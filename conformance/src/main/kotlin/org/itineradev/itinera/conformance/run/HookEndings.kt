package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookReturn
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.policy.testing.FailureHookEnding
import org.itineradev.itinera.policy.testing.HookEnding
import org.itineradev.itinera.policy.testing.WorkflowHookEnding

/** The message of a hook or a policy's build the case makes fail with none written. */
internal const val UNWRITTEN_FAILURE = "the scripted policy failed"

/** How `on step success` ends, as [returns] says. */
internal fun successEnding(returns: HookReturn): HookEnding =
    when (returns) {
        HookReturn.KeepDefault -> HookEnding.keepDefault()
        HookReturn.FinishWorkflow -> HookEnding.finishWorkflow()
        is HookReturn.FailWorkflow -> HookEnding.failWorkflow(returns.code)
        is HookReturn.Fails -> HookEnding.fails(returns.message ?: UNWRITTEN_FAILURE)
    }

/** How the step hook [hook], other than `on step success`, ends, as [returns] says. */
internal fun failureEnding(returns: HookReturn, hook: Hook): FailureHookEnding =
    when (returns) {
        HookReturn.KeepDefault -> HookEnding.keepDefault()
        HookReturn.FinishWorkflow ->
            throw ModelException("Kotlin's types refuse FinishWorkflow from \"$hook\"")
        is HookReturn.FailWorkflow -> HookEnding.failWorkflow(returns.code)
        is HookReturn.Fails -> HookEnding.fails(returns.message ?: UNWRITTEN_FAILURE)
    }

/** How the workflow hook [hook] ends, as [returns] says. */
internal fun workflowEnding(returns: HookReturn, hook: Hook): WorkflowHookEnding =
    when (returns) {
        HookReturn.KeepDefault -> HookEnding.keepDefault()
        is HookReturn.Fails -> HookEnding.fails(returns.message ?: UNWRITTEN_FAILURE)
        HookReturn.FinishWorkflow,
        is HookReturn.FailWorkflow ->
            throw ModelException("Kotlin's types refuse a lifecycle from \"$hook\"")
    }
