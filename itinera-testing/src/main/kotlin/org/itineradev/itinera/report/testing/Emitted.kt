package org.itineradev.itinera.report.testing

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.ContributionsDiscarded
import org.itineradev.itinera.event.DataOverwritten
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.GlobalMetadataSet
import org.itineradev.itinera.event.InputAdapterFailed
import org.itineradev.itinera.event.InputAdapterSupplied
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyError
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyInfo
import org.itineradev.itinera.event.JourneyStarted
import org.itineradev.itinera.event.JourneySucceeded
import org.itineradev.itinera.event.JourneyWarning
import org.itineradev.itinera.event.OptionalInputAbsent
import org.itineradev.itinera.event.ServiceProvided
import org.itineradev.itinera.event.ServiceProviderFailed
import org.itineradev.itinera.event.StepAbnormalTermination
import org.itineradev.itinera.event.StepError
import org.itineradev.itinera.event.StepFailed
import org.itineradev.itinera.event.StepGivenUp
import org.itineradev.itinera.event.StepHookCalled
import org.itineradev.itinera.event.StepInfo
import org.itineradev.itinera.event.StepRescued
import org.itineradev.itinera.event.StepRescuing
import org.itineradev.itinera.event.StepRetrying
import org.itineradev.itinera.event.StepSkipped
import org.itineradev.itinera.event.StepSucceeded
import org.itineradev.itinera.event.StepWarning
import org.itineradev.itinera.event.WorkflowHookCalled

/**
 * An event a step or a hook emitted through a reporter a recorder lent it: its [level], its
 * [message], and its [data], `null` when it gave none.
 *
 * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
 */
@JvmRecord
public data class Emitted(
    /** How serious the event is. */
    val level: Level,
    /** The message given. */
    val message: String,
    /** The data given, captured as a value when it was emitted; `null` when none was given. */
    val data: Any?,
)

/**
 * How serious an emitted event is: information, a warning or an error, as a reporter's `info`,
 * `warning` and `error` emit it.
 *
 * @sample org.itineradev.itinera.step.testing.samples.stepEventReporter
 */
public enum class Level {
    /** Information. */
    INFO,

    /** A warning. */
    WARNING,

    /** An error. */
    ERROR,
}

/**
 * What [event] emitted, or `null` for an event that a step or a hook does not emit. Every kind is
 * named, so a new kind does not compile until it says whether it is emitted.
 */
internal fun emittedOf(event: Event): Emitted? =
    when (event) {
        is StepInfo -> Emitted(Level.INFO, event.message, event.data)
        is StepWarning -> Emitted(Level.WARNING, event.message, event.data)
        is StepError -> Emitted(Level.ERROR, event.message, event.data)
        is JourneyInfo -> Emitted(Level.INFO, event.message, event.data)
        is JourneyWarning -> Emitted(Level.WARNING, event.message, event.data)
        is JourneyError -> Emitted(Level.ERROR, event.message, event.data)
        is JourneyStarted,
        is AttemptStarted,
        is StepSucceeded,
        is StepFailed,
        is StepSkipped,
        is StepAbnormalTermination,
        is StepRetrying,
        is StepGivenUp,
        is StepRescuing,
        is StepRescued,
        is JourneySucceeded,
        is JourneyFailed,
        is JourneyAborted,
        is StepHookCalled,
        is WorkflowHookCalled,
        is InputAdapterSupplied,
        is InputAdapterFailed,
        is OptionalInputAbsent,
        is ServiceProvided,
        is ServiceProviderFailed,
        is ContributionCommitted,
        is ContributionsDiscarded,
        is DataOverwritten,
        is GlobalMetadataSet -> null
    }
