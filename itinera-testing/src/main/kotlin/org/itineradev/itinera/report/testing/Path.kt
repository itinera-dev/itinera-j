package org.itineradev.itinera.report.testing

import org.itineradev.itinera.event.AttemptStarted
import org.itineradev.itinera.event.ContributionCommitted
import org.itineradev.itinera.event.ContributionsDiscarded
import org.itineradev.itinera.event.DataOverwritten
import org.itineradev.itinera.event.DecidedBy
import org.itineradev.itinera.event.DecidingHook
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.GiveUpCause
import org.itineradev.itinera.event.GlobalMetadataSet
import org.itineradev.itinera.event.InputAdapterFailed
import org.itineradev.itinera.event.InputAdapterSupplied
import org.itineradev.itinera.event.JourneyAbort
import org.itineradev.itinera.event.JourneyAborted
import org.itineradev.itinera.event.JourneyError
import org.itineradev.itinera.event.JourneyFailed
import org.itineradev.itinera.event.JourneyFailure
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
import org.itineradev.itinera.step.Reason

/**
 * The line an event of the journey's flow writes in its path, or `null` for an event outside the
 * flow. Every kind is named, so a new kind does not compile until it says whether it has a line.
 */
internal fun pathLine(event: Event): String? =
    when (event) {
        is StepSucceeded -> "${event.step} attempt ${event.attempt} succeeded"
        is StepFailed -> "${event.step} attempt ${event.attempt} failed: ${event.reason.code}"
        is StepSkipped -> "${event.step} attempt ${event.attempt} skipped" + because(event.reason)
        is StepAbnormalTermination ->
            "${event.step} attempt ${event.attempt} terminated abnormally: ${event.errorMessage}"
        is StepRetrying -> "${event.step} retrying"
        is StepGivenUp -> "${event.step} given up: ${givenUp(event.cause)}"
        is StepRescuing -> "${event.step} rescuing with ${event.rescue}"
        is StepRescued -> "${event.step} rescued by ${event.rescue}"
        is JourneySucceeded -> "journey succeeded" + decided(event.decidedBy)
        is JourneyFailed -> "journey failed at ${event.step}: ${failedFor(event.failure)}"
        is JourneyAborted -> aborted(event.abort)
        is JourneyStarted,
        is AttemptStarted,
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
        is GlobalMetadataSet,
        is StepInfo,
        is StepWarning,
        is StepError,
        is JourneyInfo,
        is JourneyWarning,
        is JourneyError -> null
    }

/** `: ` and the reason's code, or nothing when there is no reason. */
private fun because(reason: Reason?): String =
    when (reason) {
        null -> ""
        else -> ": ${reason.code}"
    }

private fun givenUp(cause: GiveUpCause): String =
    when (cause) {
        is GiveUpCause.FailWorkflow -> "FailWorkflow from ${cause.decidedBy.policy}"
        GiveUpCause.Failure,
        GiveUpCause.AbnormalTermination,
        GiveUpCause.RetriesExhausted -> cause.cause.toString()
    }

private fun decided(by: DecidedBy): String =
    when (by) {
        is DecidingHook -> ", decided by ${by.policy}"
        DecidedBy.Default -> ""
    }

/** The reason's code, or the error's message when the journey failed with no reason. */
private fun failedFor(failure: JourneyFailure): String =
    when (failure) {
        is JourneyFailure.Failure -> failure.reason.code
        is JourneyFailure.RetriesExhaustedByFailure -> failure.reason.code
        is JourneyFailure.FailWorkflow -> failure.reason.code
        is JourneyFailure.AbnormalTermination -> failure.errorMessage
        is JourneyFailure.RetriesExhaustedByAbnormalTermination -> failure.errorMessage
    }

private fun aborted(abort: JourneyAbort): String =
    when (val step = abort.step) {
        null -> "journey aborted: ${abort.reason}"
        else -> "journey aborted at $step: ${abort.reason}"
    }
