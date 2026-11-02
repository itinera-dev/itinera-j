package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookAction
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.ModelException

/** The sentences that script the facts and roles a hook requests. */
internal class HookFactSentences(private val world: World) {
    @Given("the hook {string} of policy {string} requests the step name")
    fun requestsTheStepName(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.StepName)
    }

    @Given("the hook {string} of policy {string} requests the attempt number")
    fun requestsTheAttempt(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.Attempt)
    }

    @Given("the hook {string} of policy {string} requests the journey ID")
    fun requestsTheJourneyId(hook: String, policy: String) {
        world.requests(hook, policy, HookRequest.JourneyId)
    }

    @Given(
        "the hook {string} of policy {string} requests the role {string} and calls its operation " +
            "{string}"
    )
    fun callsTheRole(hook: String, policy: String, role: String, operation: String) {
        val provided = world.model.workflow.roles[role]
        if (provided != null && provided != operation) {
            throw ModelException(
                "the role \"$role\" has the operation \"$provided\", not \"$operation\""
            )
        }
        world.requests(hook, policy, HookRequest.Role(role))
        world.does(hook, policy, HookAction.CallsRole(role, operation))
    }
}
