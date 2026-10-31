package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Ending
import org.itineradev.itinera.conformance.model.Input
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.ValueType
import org.itineradev.itinera.conformance.model.value

/**
 * The sentences that declare the workflow, its steps and rescue steps, what each step requests and
 * contributes, how each attempt ends and how often a step may retry.
 */
internal class WorkflowSentences(private val world: World) {
    private val model
        get() = world.model

    @Given("a workflow {string} with the steps:")
    fun aWorkflowWithTheSteps(workflow: String, steps: DataTable) {
        model.declare(workflow, steps.asMaps().map(::stepOf))
    }

    @Given("step {string} requests input {string} of type {word}")
    fun requestsInput(step: String, key: String, type: String) {
        model.step(step).inputs += Input(key, ValueType.named(type), optional = false)
    }

    @Given("step {string} requests optional input {string} of type {word}")
    fun requestsOptionalInput(step: String, key: String, type: String) {
        model.step(step).inputs += Input(key, ValueType.named(type), optional = true)
    }

    @Given("""^step "([^"]*)" contributes "([^"]*)" = (?!.* and then changes it to )(.+)$""")
    fun contributes(step: String, key: String, written: String) {
        model.step(step).contributions += key to value(written)
    }

    @Given("step {string} succeeds")
    fun succeeds(step: String) {
        model.step(step).attempts(step, listOf(ScriptedAttempt(1, Ending.SUCCESS)))
    }

    @Given("step {string} attempts:")
    fun attempts(step: String, attempts: DataTable) {
        model.step(step).attempts(step, attempts.asMaps().map(::attemptOf))
    }

    @Given("step {string} does not retry abnormal terminations")
    fun doesNotRetryAbnormalTerminations(step: String) {
        model.step(step).doesNotRetryAbnormalTerminations()
    }

    @Given("step {string} allows {int} retries")
    fun allowsRetries(step: String, retries: Int) {
        model.step(step).allows(step, retries)
    }

    @Given("step {string} has a rescue step")
    fun hasARescueStep(step: String) {
        model.rescue(step)
    }
}

private fun stepOf(row: Map<String?, String?>): String = cell(row, "step")
