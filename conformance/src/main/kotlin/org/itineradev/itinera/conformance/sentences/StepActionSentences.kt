package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Emitted
import org.itineradev.itinera.conformance.model.StepAction
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.NotAValue

/**
 * The sentences that script what a step does on every attempt: it contributes, emits, sets global
 * metadata and changes inputs.
 */
internal class StepActionSentences(private val world: World) {
    private val model
        get() = world.model

    @Given("""^step "([^"]*)" contributes "([^"]*)" = (?!.* and then changes it to )(.+)$""")
    fun contributes(step: String, key: String, written: String) {
        model.step(step).actions += StepAction.Contributes(key, value(written))
    }

    @Given("step {string} contributes a non-value as {string}")
    fun contributesANonValue(step: String, key: String) {
        model.step(step).actions += StepAction.Contributes(key, NotAValue)
    }

    @Given("step {string} emits {word} {string}")
    fun emits(step: String, kind: String, message: String) {
        model.step(step).actions += StepAction.Emits(Emitted.named(kind), message, null)
    }

    @Given("""^step "([^"]*)" emits (\w+) "([^"]*)" with data (?!that is not a value$)(\{.*)$""")
    fun emitsWithData(step: String, kind: String, message: String, written: String) {
        model.step(step).actions += StepAction.Emits(Emitted.named(kind), message, value(written))
    }

    @Given("step {string} emits {word} {string} with data that is not a value")
    fun emitsANonValue(step: String, kind: String, message: String) {
        model.step(step).actions += StepAction.Emits(Emitted.named(kind), message, NotAValue)
    }

    @Given("""^step "([^"]*)" sets the global metadata "([^"]*)" to (?!a )(.+)$""")
    fun setsGlobalMetadata(step: String, key: String, written: String) {
        model.step(step).actions += StepAction.SetsGlobalMetadata(key, value(written))
    }

    @Given("step {string} sets the global metadata {string} to a non-value")
    fun setsGlobalMetadataToANonValue(step: String, key: String) {
        model.step(step).actions += StepAction.SetsGlobalMetadata(key, NotAValue)
    }

    @Given("step {string} ignores failures of its emit calls and carries on")
    fun ignoresEmitFailures(step: String) {
        model.step(step).ignoresEmitFailures = true
    }

    @Given("""^step "([^"]*)" changes its input "([^"]*)" to (.+)$""")
    fun changesItsInput(step: String, key: String, written: String) {
        model.step(step).inputChanges[key] = value(written)
    }
}
