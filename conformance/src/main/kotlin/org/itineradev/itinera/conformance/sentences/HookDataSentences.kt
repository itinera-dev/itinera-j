package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.ValueType

/**
 * The sentences that script what data a hook requests: step data, data from the workflow, and what
 * the attempt left.
 */
internal class HookDataSentences(private val world: World) {
    @Given("the hook {string} of policy {string} requests step data {string} of type {word}")
    fun requestsStepData(hook: String, policy: String, key: String, type: String) {
        world.requests(
            hook,
            policy,
            HookRequest.StepData(key, ValueType.named(type), optional = false),
        )
    }

    @Given(
        "the hook {string} of policy {string} requests optional step data {string} of type {word}"
    )
    fun requestsOptionalStepData(hook: String, policy: String, key: String, type: String) {
        world.requests(
            hook,
            policy,
            HookRequest.StepData(key, ValueType.named(type), optional = true),
        )
    }

    @Given(
        "the hook {string} of policy {string} requests data from the workflow {string} of type {word}"
    )
    fun requestsWorkflowData(hook: String, policy: String, key: String, type: String) {
        world.requests(
            hook,
            policy,
            HookRequest.WorkflowData(key, ValueType.named(type), optional = false),
        )
    }

    @Given(
        "the hook {string} of policy {string} requests optional data from the workflow {string} " +
            "of type {word}"
    )
    fun requestsOptionalWorkflowData(hook: String, policy: String, key: String, type: String) {
        world.requests(
            hook,
            policy,
            HookRequest.WorkflowData(key, ValueType.named(type), optional = true),
        )
    }

    @Given("the hook {string} of policy {string} requests the failure reason")
    fun requestsTheFailureReason(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.FailureReason(optional = false))
    }

    @Given("the hook {string} of policy {string} requests the optional failure reason")
    fun requestsTheOptionalFailureReason(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.FailureReason(optional = true))
    }

    @Given("the hook {string} of policy {string} requests the error")
    fun requestsTheError(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.Error)
    }

    @Given("the hook {string} of policy {string} requests the failure cause")
    fun requestsTheFailureCause(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.FailureCause)
    }

    @Given("the hook {string} of policy {string} requests the retry cause")
    fun requestsTheRetryCause(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.RetryCause)
    }
}
