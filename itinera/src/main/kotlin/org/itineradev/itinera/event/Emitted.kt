package org.itineradev.itinera.event

import java.time.Instant
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.WorkflowName

/**
 * A step reported information.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepInfo(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The step's message. */
    val message: String,
    /**
     * The data the step gave, captured as a value when it was emitted; `null` when it gave none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.STEP_INFO
}

/**
 * A step reported a warning.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepWarning(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The step's message. */
    val message: String,
    /**
     * The data the step gave, captured as a value when it was emitted; `null` when it gave none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.STEP_WARNING
}

/**
 * A step reported an error, which does not change its outcome.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class StepError(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The step's name. */
    val step: StepName,
    /** The attempt. */
    val attempt: Attempt,
    /** The step's message. */
    val message: String,
    /**
     * The data the step gave, captured as a value when it was emitted; `null` when it gave none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.STEP_ERROR
}

/**
 * A hook or a service provider reported information.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneyInfo(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The hook or service provider that emitted it. */
    val source: EmitterSource,
    /** The author's message. */
    val message: String,
    /**
     * The data the author gave, captured as a value when it was emitted; `null` when it gave none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.JOURNEY_INFO
}

/**
 * A hook or a service provider reported a warning.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneyWarning(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The hook or service provider that emitted it. */
    val source: EmitterSource,
    /** The author's message. */
    val message: String,
    /**
     * The data the author gave, captured as a value when it was emitted; `null` when it gave none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.JOURNEY_WARNING
}

/**
 * A hook or a service provider reported an error, which does not change what it returns.
 *
 * @sample org.itineradev.itinera.event.samples.describe
 */
@JvmRecord
public data class JourneyError(
    override val sequence: Long,
    override val timestamp: Instant,
    override val journeyId: JourneyId,
    override val workflow: WorkflowName,
    /** The hook or service provider that emitted it. */
    val source: EmitterSource,
    /** The author's message. */
    val message: String,
    /**
     * The data the author gave, captured as a value when it was emitted; `null` when it gave none.
     */
    val data: Any?,
) : Event {
    override val kind: EventKind
        get() = EventKind.JOURNEY_ERROR
}
