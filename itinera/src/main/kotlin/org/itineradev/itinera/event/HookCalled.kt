package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.Lifecycle
import org.itineradev.itinera.workflow.WorkflowName

/**
 * A hook returned without failing: [StepHookCalled] for a hook of a step policy, with the lifecycle
 * it returned, or [WorkflowHookCalled] for a hook of a workflow policy, which returns none.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
public sealed interface HookCalled : Event {
    /** The hook. */
    public val source: HookSource
}

/**
 * A hook of a step policy returned without failing.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepHookCalled(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    override val source: StepHookSource,
    /** The lifecycle it returned; `null` when it kept the default. */
    val lifecycle: Lifecycle?,
) : HookCalled {
    override val kind: EventKind
        get() = EventKind.HOOK_CALLED
}

/**
 * A hook of a workflow policy returned without failing.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class WorkflowHookCalled(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    override val source: ByWorkflowHook,
) : HookCalled {
    override val kind: EventKind
        get() = EventKind.HOOK_CALLED
}
