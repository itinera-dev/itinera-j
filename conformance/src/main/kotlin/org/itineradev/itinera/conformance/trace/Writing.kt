package org.itineradev.itinera.conformance.trace

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
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName

/** The line of [event], with every column it fills. */
internal fun lineOf(event: Event): Line = Writing().apply { write(event) }.lineOf(event)

/** A line being written, one column after another. */
private class Writing {
    private val cells = linkedMapOf<String, String>()
    private val carried = mutableListOf<Any?>()
    private var data: Data = Data.None

    fun lineOf(event: Event): Line =
        Line(event.kind.toString(), cells.toMap(), data, carried.toList())

    /**
     * Writes [event]'s columns, with one branch for each kind of event, so a new kind needs one.
     */
    @Suppress("CyclomaticComplexMethod")
    fun write(event: Event) {
        when (event) {
            is JourneyStarted -> Unit
            is AttemptStarted -> attempt(event.step, event.attempt)
            is StepSucceeded -> attempt(event.step, event.attempt)
            is ContributionsDiscarded -> attempt(event.step, event.attempt)
            is StepFailed -> attempted(event.step, event.attempt, event.reason)
            is StepSkipped -> attempted(event.step, event.attempt, event.reason)
            is StepAbnormalTermination -> {
                attempt(event.step, event.attempt)
                carried += event.errorMessage
            }
            is InputAdapterSupplied -> adapted(event.step, event.attempt, event.key, event.adapter)
            is InputAdapterFailed -> adapted(event.step, event.attempt, event.key, event.adapter)
            is ServiceProviderFailed -> {
                attempt(event.step, event.attempt)
                set("key", event.key)
                set("provider", event.provider)
            }
            is StepRetrying -> decided(event.step, event.attempt, event.cause, event.decidedBy)
            is StepRescuing -> {
                decided(event.step, event.attempt, event.cause, event.decidedBy)
                set("rescue", event.rescue)
            }
            is StepRescued -> {
                decided(event.step, event.attempt, null, event.decidedBy)
                set("rescue", event.rescue)
            }
            is StepGivenUp -> {
                decided(event.step, event.attempt, event.cause.cause, event.decidedBy)
                reason((event.cause as? GiveUpCause.FailWorkflow)?.reason)
            }
            is JourneySucceeded -> decidedBy(event.decidedBy)
            is JourneyFailed -> {
                set("step", event.step)
                set("cause", event.failure.cause)
                reason(event.failure.reason)
                set("error", event.failure.errorMessage)
                decidedBy(event.decidedBy)
            }
            is JourneyAborted -> cellsOf(event).forEach(::set)
            is OptionalInputAbsent -> cellsOf(event).forEach(::set)
            is ServiceProvided -> {
                cellsOf(event).forEach(::set)
                data = if (event.data == null) Data.None else Data.Is(event.data)
            }
            is StepInfo -> emitted(stepCells(event.step, event.attempt), event.message, event.data)
            is StepWarning ->
                emitted(stepCells(event.step, event.attempt), event.message, event.data)
            is StepError -> emitted(stepCells(event.step, event.attempt), event.message, event.data)
            is ContributionCommitted -> contributedCells(event.key, event.source).forEach(::set)
            is DataOverwritten -> contributedCells(event.key, event.source).forEach(::set)
            is GlobalMetadataSet -> metadataCells(event.key, event.source).forEach(::set)
            is StepHookCalled -> hookCalledCells(event).forEach(::set)
            is WorkflowHookCalled -> sourceCells(event.source).forEach(::set)
            is JourneyInfo -> emitted(sourceCells(event.source), event.message, event.data)
            is JourneyWarning -> emitted(sourceCells(event.source), event.message, event.data)
            is JourneyError -> emitted(sourceCells(event.source), event.message, event.data)
        }
    }

    private fun set(cell: Cell) {
        set(cell.first, cell.second)
    }

    private fun set(column: String, cell: Any?) {
        if (cell != null) cells[column] = cell.toString()
    }

    private fun attempt(step: StepName, attempt: Attempt) {
        set("step", step)
        set("attempt", attempt)
    }

    private fun attempted(step: StepName, attempt: Attempt, reason: Reason?) {
        attempt(step, attempt)
        reason(reason)
    }

    private fun adapted(step: StepName, attempt: Attempt, key: String, adapter: Any) {
        attempt(step, attempt)
        set("key", key)
        set("adapter", adapter)
    }

    private fun decided(step: StepName, attempt: Attempt, cause: Any?, decidedBy: DecidedBy) {
        attempt(step, attempt)
        set("cause", cause)
        decidedBy(decidedBy)
    }

    private fun reason(reason: Reason?) {
        if (reason == null) return
        set("code", reason.code)
        carried.addAll(listOfNotNull(reason.message, reason.details))
    }

    private fun decidedBy(decidedBy: DecidedBy) {
        set("decided by", writtenBy(decidedBy))
    }

    private fun emitted(source: List<Cell>, message: String, emitted: Any?) {
        source.forEach(::set)
        set("message", message)
        data = if (emitted == null) Data.None else Data.Is(emitted)
    }
}

private fun writtenBy(decidedBy: DecidedBy): String =
    when (decidedBy) {
        DecidedBy.Default -> "default"
        is DecidingHook -> "${decidedBy.policy}, ${decidedBy.hook}"
    }
