package org.itineradev.itinera.conformance.trace

import org.itineradev.itinera.event.BuildSource
import org.itineradev.itinera.event.ByAdapter
import org.itineradev.itinera.event.ByAdapterForKey
import org.itineradev.itinera.event.ByPolicy
import org.itineradev.itinera.event.ByProvider
import org.itineradev.itinera.event.ByProviderForKey
import org.itineradev.itinera.event.ByRescueStep
import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.ByStepHook
import org.itineradev.itinera.event.ByWorkflowHook
import org.itineradev.itinera.event.FailureRequester
import org.itineradev.itinera.event.HookFailed
import org.itineradev.itinera.event.InvalidLifecycle
import org.itineradev.itinera.event.JourneyAbort
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.NotAValue
import org.itineradev.itinera.event.PolicyCouldNotBeBuilt
import org.itineradev.itinera.event.ReporterFailed
import org.itineradev.itinera.event.Requester
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.RescueSkipped
import org.itineradev.itinera.event.ReservedMetadataKey
import org.itineradev.itinera.event.ResultSource
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.event.ValueSource
import org.itineradev.itinera.event.WrongData
import org.itineradev.itinera.event.WrongType

/** The key a case writes for an abort on a request for the failure's reason. */
private const val FAILURE_REASON = "failure reason"

/** The key a case writes for an abort on a request for the error. */
private const val ERROR = "error"

/**
 * The cells naming what [abort] names besides its step and its code: the key, and the adapter,
 * provider, policy or hook it blames.
 */
internal fun cellsOf(abort: JourneyAbort): List<Cell> =
    when (abort) {
        is StepCouldNotBeBuilt -> builtCells(abort.source)
        is PolicyCouldNotBeBuilt -> listOf("policy" to abort.policy)
        is RequiredDataMissing -> cellsOf(abort.missing)
        is WrongType -> cellsOf(abort.wrong)
        is NotAValue.UnderKey -> listOf("key" to abort.key) + valueCells(abort.source)
        is NotAValue.InEvent -> valueCells(abort.source)
        is NotAValue.InDetails -> valueCells(abort.source)
        is UnexpectedResult -> resultCells(abort.source)
        is ReservedMetadataKey -> listOf("key" to abort.key) + valueCells(abort.source)
        is HookFailed,
        is ReporterFailed,
        is InvalidLifecycle,
        is RescueSkipped -> emptyList()
    }

private fun cellsOf(missing: MissingData): List<Cell> =
    when (missing) {
        is MissingData.Key -> listOf("key" to missing.key) + requesterCells(missing.requester)
        is MissingData.FailureReason ->
            listOf("key" to FAILURE_REASON) + failureCells(missing.requester)
        is MissingData.Error -> listOf("key" to ERROR) + failureCells(missing.requester)
    }

private fun cellsOf(wrong: WrongData): List<Cell> =
    when (wrong) {
        is WrongData.Key -> listOf("key" to wrong.key) + requesterCells(wrong.requester)
        is WrongData.FailureReason ->
            listOf("key" to FAILURE_REASON) + failureCells(wrong.requester)
    }

private fun requesterCells(requester: Requester): List<Cell> =
    when (requester) {
        ByStep -> emptyList()
        is ByAdapter -> listOf("adapter" to requester.adapter)
        is ByProvider -> listOf("provider" to requester.provider)
        is ByStepHook -> hookCells(requester.policy, requester.hook)
        is ByWorkflowHook -> hookCells(requester.policy, requester.hook)
    }

private fun failureCells(requester: FailureRequester): List<Cell> =
    when (requester) {
        ByRescueStep -> emptyList()
        is ByStepHook -> hookCells(requester.policy, requester.hook)
    }

private fun builtCells(source: BuildSource): List<Cell> =
    when (source) {
        ByStep -> emptyList()
        is ByAdapterForKey -> listOf("adapter" to source.adapter, "key" to source.key)
        is ByProvider -> listOf("provider" to source.provider)
    }

private fun valueCells(source: ValueSource): List<Cell> =
    when (source) {
        ByStep -> emptyList()
        is ByProvider -> listOf("provider" to source.provider)
        is ByStepHook -> hookCells(source.policy, source.hook)
        is ByWorkflowHook -> hookCells(source.policy, source.hook)
    }

private fun resultCells(source: ResultSource): List<Cell> =
    when (source) {
        ByStep -> emptyList()
        is ByAdapterForKey -> listOf("adapter" to source.adapter, "key" to source.key)
        is ByProviderForKey -> listOf("provider" to source.provider, "key" to source.key)
        is ByStepHook -> hookCells(source.policy, source.hook)
        is ByPolicy -> listOf("policy" to source.policy)
    }

/** The cells naming a hook of a policy. */
internal fun hookCells(policy: Any, hook: Any): List<Cell> =
    listOf("policy" to policy, "hook" to hook)
