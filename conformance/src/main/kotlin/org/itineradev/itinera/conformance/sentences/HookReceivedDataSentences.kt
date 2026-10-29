package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.step.Reason

/** The sentences that read the data and the failure reason each hook received. */
internal class HookReceivedDataSentences(private val world: World) {
    @Then("""^the hook "([^"]*)" of policy "([^"]*)" received step data "([^"]*)" = (.+)$""")
    fun receivedStepData(hook: String, policy: String, key: String, written: String) {
        world.received(hook, policy, stepData(key), value(written))
    }

    @Then("the hook {string} of policy {string} received step data {string} absent")
    fun receivedStepDataAbsent(hook: String, policy: String, key: String) {
        world.received(hook, policy, stepData(key), null)
    }

    @Then(
        """^the hook "([^"]*)" of policy "([^"]*)" received data from the workflow "([^"]*)" = (.+)$"""
    )
    fun receivedWorkflowData(hook: String, policy: String, key: String, written: String) {
        world.received(hook, policy, workflowData(key), value(written))
    }

    @Then("the hook {string} of policy {string} received data from the workflow {string} absent")
    fun receivedWorkflowDataAbsent(hook: String, policy: String, key: String) {
        world.received(hook, policy, workflowData(key), null)
    }

    @Then("the hook {string} of policy {string} received no failure reason")
    fun receivedNoFailureReason(hook: String, policy: String) {
        world.received(hook, policy, ::isReason, null)
    }

    @Then(
        """^the hook "([^"]*)" of policy "([^"]*)" received the failure reason with code "([^"]*)", """ +
            """message "([^"]*)" and details (\{.*\})$"""
    )
    fun receivedTheFailureReason(
        hook: String,
        policy: String,
        code: String,
        message: String,
        details: String,
    ) {
        world.received(hook, policy, ::isReason, Reason(code, message, value(details)))
    }
}
