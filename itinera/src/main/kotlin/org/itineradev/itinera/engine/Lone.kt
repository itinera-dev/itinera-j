package org.itineradev.itinera.engine

import java.time.Clock
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.ByStepHook
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.StepHookSource
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepHook
import org.itineradev.itinera.report.DefaultDispatcherFactory
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.workflow.WorkflowName

/**
 * Handles lent outside any journey, made by the engine's own code, so that each call behaves as in
 * a journey: what they deliver is kept, unless they are [failing], when every call is interrupted
 * and nothing is kept, and setting the reserved entry `journey_id` interrupts that call and every
 * later one. They are lent to a step's attempt 1, or to a hook acting on it.
 *
 * Part of itinera's engine, not of its API.
 */
public class Lone(failing: Boolean) {
    private val delivered = Delivered(failing)
    private val journey =
        Journey(
            Emitter(JourneyId.of(LONE), WorkflowName.of(LONE), Clock.systemUTC()),
            delivered,
            Failures(),
            SyncParts(DefaultDispatcherFactory()),
            DataBag(),
        )
    private val contributions = Contributions(journey, STEP, ByStep, "The test")

    /** The contributor lent. */
    public val contributor: Contributor
        get() = contributions

    /** What was contributed, under each key in the order the keys were first given. */
    public val contributed: Map<String, Captured>
        get() = contributions.contributed

    /** What a step's reporter emits and sets global metadata through. */
    public val stepEmits: Emits = StepEmits(journey, STEP, FIRST)

    /** What a hook's reporter emits and sets global metadata through. */
    public val hookEmits: Emits =
        JourneyEmits(
            journey,
            STEP,
            ByStepHook(POLICY, HOOK),
            StepHookSource(POLICY, HOOK, STEP, FIRST),
            "The hook has returned, so its hook reporter reports nothing.",
        )

    /** The synchronous step reporter lent, which reports through [stepEmits]. */
    public val stepReporter: StepReporter = EmittingReporter(stepEmits, Handle.STEP_REPORTER)

    /** The synchronous hook reporter lent, which reports through [hookEmits]. */
    public val hookReporter: HookReporter = EmittingReporter(hookEmits, Handle.HOOK_REPORTER)

    /** Every event delivered, in order. */
    public val events: List<Event>
        get() = delivered.events()

    /** Each entry of global metadata set, with its latest value, in the order first set. */
    public val globalMetadata: Map<String, Any?>
        get() = delivered.globalMetadata()
}

/** The dispatcher of a lone journey, which keeps what it is given, or fails on it. */
private class Delivered(private val failing: Boolean) : Delivery {
    private val events = mutableListOf<Event>()
    private val metadata = LinkedHashMap<String, Any?>()

    @Synchronized fun events(): List<Event> = events.toList()

    @Synchronized fun globalMetadata(): Map<String, Any?> = metadata.toMap()

    @JvmSynthetic override suspend fun add(reporter: ReporterOf<*>, guard: Guard) = Unit

    @JvmSynthetic
    override suspend fun dispatch(event: Event) {
        delivering { events += event }
    }

    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        delivering { metadata[key] = value }
    }

    private fun delivering(keep: () -> Unit) {
        check(!failing) { "The recorder is failing." }
        synchronized(this, keep)
    }
}

private const val LONE = "test"

private val STEP: StepName = StepName.of("step")

private val POLICY: PolicyName = PolicyName.of("policy")

private val HOOK: StepHook = StepHook.ON_STEP_SUCCESS
