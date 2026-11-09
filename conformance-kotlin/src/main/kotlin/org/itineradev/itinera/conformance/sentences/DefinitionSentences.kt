package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.HookReturn
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.PolicyKind
import org.itineradev.itinera.conformance.model.ValueType

private val hookColumns = setOf("policy", "hook", "returns", "code", "fails with")

private val requestColumns = setOf("policy", "hook", "request", "key", "type", "optional")

/**
 * The sentences that define policies and their hooks, and what the hooks request, in tables of one
 * row for each hook or request.
 */
internal class DefinitionSentences(private val world: World) {
    @Given("the step policies and their hooks:")
    fun theStepPoliciesAndTheirHooks(hooks: DataTable) {
        hooks.asMaps().forEach { row -> define(PolicyKind.STEP, row) }
    }

    @Given("the workflow policies and their hooks:")
    fun theWorkflowPoliciesAndTheirHooks(hooks: DataTable) {
        hooks.asMaps().forEach { row -> define(PolicyKind.WORKFLOW, row) }
    }

    @Given("the hooks request:")
    fun theHooksRequest(requests: DataTable) {
        requests.asMaps().forEach(::request)
    }

    private fun define(kind: PolicyKind, row: Map<String?, String?>) {
        only(hookColumns, row)
        val policy = cell(row, "policy")
        val hook = cell(row, "hook")
        world.model.blocks.define(policy, kind, Hook.named(hook))
        val returns = row["returns"]
        val failsWith = row["fails with"]
        if (row["code"] != null && returns != "FailWorkflow") {
            throw ModelException("the hook \"$hook\" of \"$policy\" has a code but no FailWorkflow")
        }
        if (returns != null) world.ends(hook, policy, returned(returns, row))
        if (failsWith != null) world.ends(hook, policy, HookReturn.Fails(failsWith))
    }

    private fun request(row: Map<String?, String?>) {
        only(requestColumns, row)
        world.requests(cell(row, "hook"), cell(row, "policy"), requestOf(row))
    }
}

private fun returned(written: String, row: Map<String?, String?>): HookReturn =
    when (written) {
        "FailWorkflow" -> HookReturn.FailWorkflow(cell(row, "code"))
        "FinishWorkflow" -> HookReturn.FinishWorkflow
        else -> throw ModelException("$written is not what a hook of the cases returns")
    }

private fun requestOf(row: Map<String?, String?>): HookRequest {
    val optional = row["optional"]?.let(::isYes) ?: false
    return when (val request = cell(row, "request")) {
        "step data" -> HookRequest.StepData(cell(row, "key"), typeOf(row), optional)
        "data from the workflow" ->
            HookRequest.WorkflowData(cell(row, "key"), typeOf(row), optional)
        "failure reason" -> HookRequest.FailureReason(optional)
        else -> if (optional) noOptionalForm(request) else requiredOf(request)
    }
}

private fun typeOf(row: Map<String?, String?>): ValueType = ValueType.named(cell(row, "type"))

private fun requiredOf(request: String): HookRequest =
    when (request) {
        "failure cause" -> HookRequest.FailureCause
        "retry cause" -> HookRequest.RetryCause
        "error" -> HookRequest.Error
        "step name" -> HookRequest.StepName
        "attempt number" -> HookRequest.Attempt
        "journey ID" -> HookRequest.JourneyId
        else -> throw ModelException("$request is not a request of a hook of the cases")
    }

private fun noOptionalForm(request: String): Nothing =
    throw ModelException("the request $request has no optional form")
