package org.itineradev.itinera.conformance.model

import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.policy.WorkflowHook

/** A hook, as a case names it: a step hook or a workflow hook. */
internal sealed interface Hook {
    /** The step hook [hook]. */
    data class OfStep(val hook: StepHook) : Hook {
        override fun toString(): String = hook.toString()
    }

    /** The workflow hook [hook]. */
    data class OfWorkflow(val hook: WorkflowHook) : Hook {
        override fun toString(): String = hook.toString()
    }

    companion object {
        private val byName =
            StepHook.entries.associateBy(StepHook::toString).mapValues(::ofStep) +
                WorkflowHook.entries.associateBy(WorkflowHook::toString).mapValues(::ofWorkflow)

        /** The hook a case names. */
        fun named(written: String): Hook =
            byName[written] ?: throw ModelException("$written is not a hook of the cases")
    }
}

private fun ofStep(entry: Map.Entry<String, StepHook>): Hook = Hook.OfStep(entry.value)

private fun ofWorkflow(entry: Map.Entry<String, WorkflowHook>): Hook = Hook.OfWorkflow(entry.value)

/** What a scripted hook requests, in the order its sentences are written. */
internal sealed interface HookRequest {
    /** Data the step contributed under [key], of [type]. */
    data class StepData(val key: String, val type: ValueType, val optional: Boolean) : HookRequest

    /** Data from the workflow under [key], of [type]. */
    data class WorkflowData(val key: String, val type: ValueType, val optional: Boolean) :
        HookRequest

    /** The failure's reason. */
    data class FailureReason(val optional: Boolean) : HookRequest

    /** The error that ended the attempt. */
    data object Error : HookRequest

    /** Why `on step failure` was called. */
    data object FailureCause : HookRequest

    /** Why `on step retry` was called. */
    data object RetryCause : HookRequest

    /** The name of the step the hook acts on. */
    data object StepName : HookRequest

    /** The attempt the hook acts on. */
    data object Attempt : HookRequest

    /** The journey ID. */
    data object JourneyId : HookRequest

    /** The role [role]. */
    data class Role(val role: String) : HookRequest
}

/** What a scripted hook does before it ends, in the order its sentences are written. */
internal sealed interface HookAction {
    /** It contributes [value] under [key]. */
    data class Contributes(val key: String, val value: Any?) : HookAction

    /** It contributes, under [key], how many calls of the policy's hooks its instance has seen. */
    data class CountsCalls(val key: String) : HookAction

    /** It changes the step data it received under [key] to [value], in place. */
    data class ChangesStepData(val key: String, val value: Any?) : HookAction

    /** It emits [kind] with [message]. */
    data class Emits(val kind: JourneyEmitted, val message: String) : HookAction

    /** It sets the global metadata [key] to [value]. */
    data class SetsGlobalMetadata(val key: String, val value: Any?) : HookAction

    /** It calls [operation] of the role [role] it requested. */
    data class CallsRole(val role: String, val operation: String) : HookAction

    /**
     * It contributes [value] under [key] and emits `step_info` [message] through the contributor
     * and the step reporter [step] kept after its attempt ended.
     */
    data class UsesKeptHandles(
        val step: String,
        val key: String,
        val value: Any?,
        val message: String,
    ) : HookAction
}

/** How a scripted hook ends. */
internal sealed interface HookReturn {
    /** It keeps the default, or, for a workflow hook, returns. */
    data object KeepDefault : HookReturn

    /** It returns `FinishWorkflow`. */
    data object FinishWorkflow : HookReturn

    /** It returns `FailWorkflow` with a reason of [code]. */
    data class FailWorkflow(val code: String) : HookReturn

    /** It fails, with [message] when the case writes one. */
    data class Fails(val message: String?) : HookReturn
}

/** The events a hook emits through its hook reporter, as a case names them. */
internal enum class JourneyEmitted(val written: String) {
    JOURNEY_INFO("journey_info"),
    JOURNEY_WARNING("journey_warning"),
    JOURNEY_ERROR("journey_error");

    companion object {
        private val byName = entries.associateBy(JourneyEmitted::written)

        /** The event a case names. */
        fun named(written: String): JourneyEmitted =
            byName[written] ?: throw ModelException("a hook cannot emit $written")
    }
}

/** What a scenario scripts for one hook of a policy, filled in as it is written. */
internal class HookScript {
    val requests = mutableListOf<HookRequest>()
    val actions = mutableListOf<HookAction>()

    /** How the hook ends, once stated; it keeps the default until then. */
    var returns: HookReturn = HookReturn.KeepDefault
        private set

    private var returnStated = false

    /** Says how the hook ends, once. */
    fun returns(hook: Hook, policy: String, ending: HookReturn) {
        if (returnStated) {
            throw ModelException(
                "how the hook \"$hook\" of policy \"$policy\" ends is stated twice"
            )
        }
        returnStated = true
        returns = ending
    }
}
