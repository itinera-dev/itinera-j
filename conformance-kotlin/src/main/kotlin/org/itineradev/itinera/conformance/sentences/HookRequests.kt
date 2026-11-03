package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.model.HookRequest

internal fun stepData(key: String): (HookRequest) -> Boolean = { request ->
    request is HookRequest.StepData && request.key == key
}

internal fun workflowData(key: String): (HookRequest) -> Boolean = { request ->
    request is HookRequest.WorkflowData && request.key == key
}

internal fun isStepName(request: HookRequest): Boolean = request == HookRequest.StepName

internal fun isAttempt(request: HookRequest): Boolean = request == HookRequest.Attempt

internal fun isJourneyId(request: HookRequest): Boolean = request == HookRequest.JourneyId

internal fun isError(request: HookRequest): Boolean = request == HookRequest.Error

internal fun isReason(request: HookRequest): Boolean = request is HookRequest.FailureReason

internal fun isCause(request: HookRequest): Boolean =
    request == HookRequest.FailureCause || request == HookRequest.RetryCause
