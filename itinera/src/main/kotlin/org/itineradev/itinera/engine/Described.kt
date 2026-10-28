package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.HookCall
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.policy.SyncWorkflowHook
import org.itineradev.itinera.request.HookReporterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.StepKind
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The descriptors of workflows written as annotated classes, each read once per class and mode.
 *
 * Part of itinera's engine, not of its API.
 */
public object Described {
    private val synchronous =
        object : ClassValue<WorkflowDescriptor<*, Sync>>() {
            override fun computeValue(type: Class<*>): WorkflowDescriptor<*, Sync> =
                read<Any?, Sync>(type, Sync, SynchronousSteps, SynchronousHooks)
        }

    /**
     * The descriptor of the synchronous workflow [type] declares.
     *
     * @throws Violations with every mistake in its declaration.
     */
    public fun <W : ItineraWorkflow> synchronous(type: Class<W>): WorkflowDescriptor<W, Sync> =
        returned(synchronous.get(type))

    /**
     * Reads the workflow [type] declares in [mode], whose steps [steps] makes factories for, and
     * whose policies' hooks [hooks] calls.
     *
     * @throws Violations with every mistake in its declaration.
     */
    public fun <W, M : Mode> read(
        type: Class<*>,
        mode: M,
        steps: StepFactories<M>,
        hooks: HookCalls<M>,
    ): WorkflowDescriptor<W, M> = Reading(type, mode, steps, hooks).descriptor()
}

/**
 * What reading step classes takes in one mode: the handles of that mode a step's `run` requests by
 * their types, and the factory of each step class, which builds the step with its constructor and
 * runs it with its method `run`.
 *
 * Part of itinera's engine, not of its API.
 */
public interface StepFactories<M : Mode> {
    /**
     * The handles of this mode that a step's `run` requests by their types, beside the contributor.
     */
    public val handles: Map<Class<*>, Request<StepKind<M>, *>>

    /**
     * Why a step's `run` cannot request each handle of the other mode, by the handle's class name,
     * since this mode's artifact may not hold the class.
     */
    public val otherModes: Map<String, String>

    /** The factory of [step]. */
    public fun of(step: StepClass<M>): StepFactory<M>
}

private object SynchronousSteps : StepFactories<Sync> {
    override val handles: Map<Class<*>, Request<StepKind<Sync>, *>> =
        mapOf(StepReporter::class.java to Requests.stepReporter())

    override val otherModes: Map<String, String> =
        mapOf(
            "org.itineradev.itinera.step.async.AsyncStepReporter" to
                "requests AsyncStepReporter, which only an asynchronous step can"
        )

    override fun of(step: StepClass<Sync>): StepFactory<Sync> = SyncStepFactory { got ->
        val built = step.built(got)
        val arguments = step.arguments(got)
        SyncStepRun { returned(invoked(step.run, built, *arguments)) }
    }
}

private object SynchronousHooks : HookCalls<Sync> {
    override val handles: Map<Class<*>, Request<HookReporterKind<Sync>, *>> =
        mapOf(HookReporter::class.java to Requests.hookReporter())

    override val otherModes: Map<String, String> =
        mapOf(
            "org.itineradev.itinera.policy.async.AsyncHookReporter" to
                "requests AsyncHookReporter, which only an asynchronous hook can"
        )

    override fun <K, R> stepHook(hook: HookMethod): HookCall<Sync, Any, K, R> =
        SyncHook { policy, got ->
            returned(invoked(hook.method, policy, *hook.arguments(got)))
        }

    override fun <K> workflowHook(hook: HookMethod): HookCall<Sync, Any, K, Unit> =
        SyncWorkflowHook { policy, got ->
            invoked(hook.method, policy, *hook.arguments(got))
        }
}
