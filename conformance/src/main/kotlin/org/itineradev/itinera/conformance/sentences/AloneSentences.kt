package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.adapterCalledAlone
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.hookRunAlone
import org.itineradev.itinera.conformance.run.stepRunAlone
import org.itineradev.itinera.workflow.Supplied

/**
 * The sentences of test support's recorders, which lend a step, a hook or an input adapter its
 * handles outside a journey, and what they kept.
 */
internal class AloneSentences(private val world: World) {
    @When("step {string} runs alone")
    fun stepRunsAlone(step: String) {
        world.ranAlone = stepRunAlone(world.model, step, world.column)
    }

    @When("the hook {string} of policy {string} runs alone")
    fun hookRunsAlone(hook: String, policy: String) {
        world.ranAlone = hookRunAlone(world.model, world.witness, policy, hook, world.column)
    }

    @When(
        "the input adapter {string} is called alone for the input {string} of step {string}, with a data bag holding:"
    )
    fun adapterIsCalledAlone(adapter: String, input: String, step: String, table: DataTable) {
        val values = table.asMaps().map(::entryOf)
        world.returned = adapterCalledAlone(world.model, adapter, step, input, values)
    }

    @Given("the step event reporter is failing")
    fun stepRecorderFails() {
        world.model.testing.failStepRecorder()
    }

    @Given("the hook event reporter is failing")
    fun hookRecorderFails() {
        world.model.testing.failHookRecorder()
    }

    @Then("the input adapter returned {}")
    fun adapterReturned(written: String) {
        val returned = world.returned
        val expected = value(written)
        holds(
            returned is Supplied.Value && returned.value == expected,
            "the input adapter to return $written",
            "$returned",
        )
    }
}
